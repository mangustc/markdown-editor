package com.example.markdown_editor.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.data.util.LinkPreviewFetcher
import com.example.markdown_editor.domain.parser.MarkdownParser
import com.example.markdown_editor.ui.editor.EditorEvent
import com.example.markdown_editor.ui.editor.MarkdownAnnotator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        NoteDb::class.java, "database-notes"
    )
        .fallbackToDestructiveMigration(true)
        .build()

    private val repository = ProjectRepositoryImpl(
        context = application,
        noteDao = db.noteDao(),
        linkPreviewDao = db.linkPreviewDao(),
    )

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadSavedProject()
    }

    sealed class NavigationEvent {
        data class GoToEditor(val note: Note) : NavigationEvent()
        object GoBack : NavigationEvent()
        object OpenDrawer : NavigationEvent()
        object CloseDrawer : NavigationEvent()
    }

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    fun openDrawer() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.OpenDrawer) }
    }

    fun goBack() {
        viewModelScope.launch { _navigationEvents.send(NavigationEvent.GoBack) }
    }

    fun onProjectSelected(uri: Uri) {
        viewModelScope.launch {
            val name = uri.lastPathSegment?.substringAfterLast(":") ?: "Project"
            val project = repository.buildProject(uri, name)
            repository.saveProject(project)
            _uiState.update { it.copy(project = project) }
            repository.syncDatabase(project)
            onSearchQueryChanged()
        }
    }

    fun onNoteSelected(note: Note) {
        viewModelScope.launch {
            _navigationEvents.send(NavigationEvent.CloseDrawer)
            _navigationEvents.send(NavigationEvent.GoToEditor(note))
        }
    }

    fun showCreateNoteDialog() {
        _uiState.update { it.copy(isCreateNoteDialogVisible = true) }
    }

    fun dismissCreateNoteDialog() {
        _uiState.update { it.copy(isCreateNoteDialogVisible = false, newNoteNameInput = "") }
    }

    fun updateNewNoteName(name: String) {
        _uiState.update { it.copy(newNoteNameInput = name) }
    }

    fun onCreateNote() {
        val project = _uiState.value.project ?: return
        val nameToUse = _uiState.value.newNoteNameInput
        viewModelScope.launch {
            val uri = repository.createNote(project, nameToUse)
            if (uri != null) {
                repository.syncDatabase(project)
                onSearchQueryChanged()
            }
        }
    }

    private var searchJob: Job? = null
    fun onSearchQueryChanged(raw: String? = null) {
        if (raw != null) _uiState.update { it.copy(searchQuery = raw) }
        val searchQuery = _uiState.value.searchQuery
        val project = _uiState.value.project ?: return

        val parsedInit = SearchQuery.parse(searchQuery.trim())
        val parsed =
            parsedInit.copy(negatedTagFilters = parsedInit.negatedTagFilters + "quick-note")

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            repository.syncDatabase(project)
            val results = repository.getNotes(project, parsed)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun editorOnEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.InsertSyntax -> editorInsertSyntax(event.syntax, event.cursorOffset)
            is EditorEvent.AttachPhoto -> editorHandleAttachPhoto(event)
            is EditorEvent.AttachFile -> editorHandleAttachFile(event)
        }
    }

    private fun editorHandleAttachPhoto(event: EditorEvent.AttachPhoto) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val relativePath = repository.copyToAssets(project = project, assetUri = event.uri)
            val markdown = "![image](<$relativePath>)"
            withContext(Dispatchers.Main) { editorInsertMarkdown(markdown) }
        }
    }

    private fun editorHandleAttachFile(event: EditorEvent.AttachFile) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val relativePath = repository.copyToAssets(project = project, assetUri = event.uri)
            val label = event.displayName ?: relativePath.substringAfterLast("/")
            val markdown = "[$label](<$relativePath>)"
            withContext(Dispatchers.Main) { editorInsertMarkdown(markdown) }
        }
    }

    private fun editorInsertMarkdown(markdown: String) {
        val current = _uiState.value.editorTextFieldValue
        val cursor = current.selection.start
        val newText = current.text.substring(0, cursor) + markdown + current.text.substring(cursor)
        val newValue = current.copy(text = newText, selection = TextRange(cursor + markdown.length))
        editorOnContentChanged(newValue)
    }

    private fun editorInsertSyntax(syntax: String, cursorOffset: Int) {
        val current = _uiState.value.editorTextFieldValue
        val cursor = current.selection.start
        val newText = current.text.substring(0, cursor) + syntax + current.text.substring(cursor)
        val newValue = current.copy(text = newText, selection = TextRange(cursor + cursorOffset))
        editorOnContentChanged(newValue)
    }

    fun editorOnContentChanged(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.editorTextFieldValue.text
        if (!textChanged) {
            _uiState.update { it.copy(editorTextFieldValue = newValue) }
            return
        }
        val spans = MarkdownParser.parse(newValue.text)
        val annotated = MarkdownAnnotator.annotate(newValue.text, spans)
        _uiState.update {
            it.copy(
                editorTextFieldValue = newValue,
                editorAnnotatedString = annotated,
                editorSpans = spans,
            )
        }
    }

    fun editorOnNoteOpened(noteUriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val note = repository.getNoteByUri(noteUriString.toUri())
                val text = repository.getNoteText(note)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(activeNote = note) }
                    editorOnContentChanged(TextFieldValue(text))
                }
            } catch (e: Exception) {
                goBack()
            }
        }
    }

    fun editorOnSave() {
        val project = _uiState.value.project ?: return
        val note = _uiState.value.activeNote ?: return
        val text = _uiState.value.editorTextFieldValue.text
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNoteText(note, text)
            repository.syncDatabase(project)
        }
    }

    fun messengerOnMessengerOpened(project: Project) {
        viewModelScope.launch {
            repository.syncDatabase(project)
            val notes = repository.getNotes(
                project,
                SearchQuery(tagFilters = listOf("quick-note")),
                includeText = true,
                includeFrontMatter = false
            )
            _uiState.update { it.copy(messengerNotesList = notes, messengerIsLoading = false) }

            // Pre-warm in-memory cache from DB for all URLs in all notes
            notes.forEach { note ->
                LinkPreviewFetcher.extractAllUrls(note.body ?: "")
                    .forEach { messengerEnsureLinkPreview(it) }
            }
        }
    }

    fun messengerOnNewNoteTextChanged(text: String) {
        _uiState.update { it.copy(messengerNewNoteText = text) }
    }

    fun messengerOnSendNote() {
        val project = _uiState.value.project ?: return
        val text = _uiState.value.messengerNewNoteText.trim()
        if (text.isBlank()) return

        val timestamp = java.time.format.DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.now())
        val name = "quick-note-$timestamp"
        val tags = listOf("quick-note")

        viewModelScope.launch(Dispatchers.IO) {
            val uri = repository.createNote(project, name, tags)
            if (uri != null) {
                val newNote = repository.getNoteByUri(uri)
                val currentContent = repository.getNoteText(newNote, includeFrontMatter = true)
                repository.saveNoteText(newNote, "$currentContent\n\n$text")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(messengerNewNoteText = "") }
                }
                messengerOnMessengerOpened(project)
                onSearchQueryChanged()
            }
        }
    }

    /**
     * Ensures a link preview for [url] is in the in-memory state map.
     *
     * Priority:
     *  1. State map already has key → skip (covers both hits and known-failures)
     *  2. Room DB has a row → load into state, no network
     *  3. Neither → fetch network, persist to DB, load into state
     *
     * Failed network fetches write nothing to DB so a fresh app launch will retry.
     * Within a session they stay null in the state map to avoid hammering the network.
     */
    fun messengerEnsureLinkPreview(url: String) {
        if (_uiState.value.messengerLinkPreviews.containsKey(url)) return

        // Reserve slot immediately; blocks concurrent duplicate launches
        _uiState.update { it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to null)) }

        viewModelScope.launch {
            // Step 2: DB
            val cached = repository.getCachedLinkPreview(url)
            if (cached != null) {
                _uiState.update {
                    it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to cached))
                }
                return@launch
            }

            // Step 3: network
            val fetched = LinkPreviewFetcher.fetch(url)
            if (fetched != null) {
                repository.saveLinkPreview(fetched)
                _uiState.update {
                    it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to fetched))
                }
            }
        }
    }

    private fun loadSavedProject() {
        viewModelScope.launch {
            val project = repository.loadSavedProject()
            if (project != null) {
                _uiState.update { it.copy(project = project) }
                onSearchQueryChanged()
            }
        }
    }

    fun showNoteDeleteDialog(note: Note) {
        _uiState.update { it.copy(isNoteDeleteDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteDeleteDialog() {
        _uiState.update { it.copy(isNoteDeleteDialogVisible = false, dialogNote = null) }
    }

    fun onDeleteNote(note: Note) {
        viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            val activeNote = _uiState.value.activeNote
            repository.deleteNote(note)
            repository.syncDatabase(project)
            onSearchQueryChanged()
            if (note.uri == activeNote?.uri) goBack()
        }
    }

    fun showNoteShowInfoDialog(note: Note) {
        _uiState.update { it.copy(isNoteShowInfoDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteShowInfoDialog() {
        _uiState.update { it.copy(isNoteShowInfoDialogVisible = false, dialogNote = null) }
    }

    fun showNoteRenameDialog(note: Note) {
        _uiState.update {
            it.copy(
                isNoteRenameDialogVisible = true,
                noteRenameInput = note.name,
                dialogNote = note
            )
        }
    }

    fun dismissNoteRenameDialog() {
        _uiState.update {
            it.copy(isNoteRenameDialogVisible = false, noteRenameInput = "", dialogNote = null)
        }
    }

    fun onRenameNameInputChanged(newName: String) {
        _uiState.update { it.copy(noteRenameInput = newName) }
    }

    fun onRenameNote(note: Note, newName: String) {
        viewModelScope.launch {
            val project = _uiState.value.project ?: return@launch
            repository.renameNote(note, newName)
            repository.syncDatabase(project)
            onSearchQueryChanged()
        }
    }
}