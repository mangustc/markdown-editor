package com.example.markdown_editor.domain.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.room.Room
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.model.SortBy
import com.example.markdown_editor.data.repository.LinkPreviewRepositoryImpl
import com.example.markdown_editor.data.repository.NoteRepositoryImpl
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.domain.editor.EditorEvent
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.messenger.Attachment
import com.example.markdown_editor.domain.messenger.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        NoteDb::class.java, "database-notes",
    )
        .fallbackToDestructiveMigration(true)
        .build()

    private val projectRepository = ProjectRepositoryImpl(
        context = application,
        noteDao = db.noteDao(),
    )
    private val noteRepository = NoteRepositoryImpl(
        context = application,
    )
    private val linkPreviewRepository = LinkPreviewRepositoryImpl(
        linkPreviewDao = db.linkPreviewDao(),
    )

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val project = ProjectActions()

    inner class ProjectActions {
        fun onProjectSelected(uri: Uri) {
            viewModelScope.launch {
                val name = uri.lastPathSegment?.substringAfterLast(":") ?: "Project"
                val project = projectRepository.buildProject(uri, name)
                projectRepository.saveProject(project)
                _uiState.update { it.copy(project = project) }
                projectRepository.syncDatabase(project)
                updateNoteLists()
            }
        }

        fun loadSavedProject() {
            viewModelScope.launch {
                val project = projectRepository.loadSavedProject()
                if (project != null) {
                    _uiState.update { it.copy(project = project) }
                    updateNoteLists()
                } else {
                    _uiState.update { it.copy(messengerIsLoading = false) }
                }
            }
        }

    }


    init {
        project.loadSavedProject()
    }

    sealed class NavigationEvent {
        data class GoToEditor(val note: Note) : NavigationEvent()
        object GoBack : NavigationEvent()
        object OpenDrawer : NavigationEvent()
        object CloseDrawer : NavigationEvent()
    }

    val navigation = NavigationActions()

    inner class NavigationActions {
        private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
        val navigationEvents = _navigationEvents.receiveAsFlow()

        val searchResultsPaged: Flow<PagingData<Note>> = _uiState
            .map { it.project to it.searchQuery }
            .distinctUntilChanged()
            .flatMapLatest { (project, queryStr) ->
                if (project == null) return@flatMapLatest emptyFlow()
                val parsedInit = SearchQuery.parse(queryStr.trim())
                val parsed = parsedInit.copy(
                    negatedTagFilters = parsedInit.negatedTagFilters + "quick-note",
                    pinnedFirst = true,
                )
                projectRepository.getNotesPaged(project, parsed)
            }
            .cachedIn(viewModelScope)

        fun openDrawer() {
            viewModelScope.launch { _navigationEvents.send(NavigationEvent.OpenDrawer) }
        }

        fun goBack() {
            viewModelScope.launch { _navigationEvents.send(NavigationEvent.GoBack) }
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
                val uri = noteRepository.createNote(project, nameToUse)
                if (uri != null) {
                    projectRepository.syncDatabase(project)
                    updateNoteLists()
                }
            }
        }

        fun onSearchQueryChanged(raw: String? = null, afterUpdate: () -> Unit = {}) {
            if (raw != null) _uiState.update { it.copy(searchQuery = raw) }
            val project = _uiState.value.project ?: return
            viewModelScope.launch {
                projectRepository.syncDatabase(project)
                afterUpdate()
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
                noteRepository.deleteNote(note)
                projectRepository.syncDatabase(project)
                updateNoteLists()
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
                    dialogNote = note,
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
                noteRepository.renameNote(note, newName)
                projectRepository.syncDatabase(project)
                updateNoteLists()
            }
        }

        fun onPinNote(note: Note) {
            viewModelScope.launch {
                val project = _uiState.value.project ?: return@launch
                noteRepository.toggleNotePin(note)
                projectRepository.syncDatabase(project)
                updateNoteLists()
            }
        }
    }

    val editor = EditorActions()

    @OptIn(FlowPreview::class)
    inner class EditorActions {
        val state = TextFieldState()

        init {
            viewModelScope.launch {
                snapshotFlow { state.text }
                    .debounce(2_000)
                    .collect { editorOnSave() }
            }
        }

        @OptIn(ExperimentalFoundationApi::class)
        fun editorOnEvent(event: EditorEvent) {
            when (event) {
                is EditorEvent.InsertSyntax -> editorInsertSyntax(event.syntax, event.cursorOffset)
                is EditorEvent.AttachPhoto -> editorHandleAttachPhoto(event)
                is EditorEvent.AttachFile -> editorHandleAttachFile(event)
                is EditorEvent.Undo -> {
                    state.undoState.undo()
                }

                is EditorEvent.Redo -> {
                    state.undoState.redo()
                }
            }
        }

        private fun editorInsertSyntax(syntax: String, cursorOffset: Int) {
            state.edit {
                val start = selection.start
                replace(start, start, syntax)
                placeCursorAfterCharAt(start + cursorOffset - 1)
            }
        }

        fun editorOnNoteOpened(noteUriString: String) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val note = noteRepository.getNoteByUri(noteUriString.toUri())
                    val text = noteRepository.getNoteText(note)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(activeNote = note) }
                        state.edit {
                            replace(0, length, text)
                        }
                    }
                } catch (e: Exception) {
                    navigation.goBack()
                }
            }
        }

        private fun editorHandleAttachPhoto(event: EditorEvent.AttachPhoto) {
            val project = _uiState.value.project ?: return
            viewModelScope.launch(Dispatchers.IO) {
                val relativePath =
                    projectRepository.copyToAssets(project = project, assetUri = event.uri)
                val markdown = "![image](<$relativePath>)"
                withContext(Dispatchers.Main) { editorInsertSyntax(markdown, 0) }
            }
        }

        private fun editorHandleAttachFile(event: EditorEvent.AttachFile) {
            val project = _uiState.value.project ?: return
            viewModelScope.launch(Dispatchers.IO) {
                val relativePath =
                    projectRepository.copyToAssets(project = project, assetUri = event.uri)
                val label = event.displayName ?: relativePath.substringAfterLast("/")
                val markdown = "[$label](<$relativePath>)"
                withContext(Dispatchers.Main) { editorInsertSyntax(markdown, 0) }
            }
        }

        fun editorOnSave() {
            val project = _uiState.value.project ?: return
            val note = _uiState.value.activeNote ?: return
            val text = state.text.toString()
            viewModelScope.launch(Dispatchers.IO) {
                noteRepository.saveNoteText(note, text)
                projectRepository.syncDatabase(project)
                updateNoteLists()
                _uiState.update { it.copy(editorSavedVersion = it.editorVersion) }
            }
        }

    }

    val messenger = MessengerActions()

    inner class MessengerActions {
        val notesPaged: Flow<PagingData<Note>> = _uiState
            .map { it.project }
            .distinctUntilChanged()
            .flatMapLatest { project ->
                if (project == null) return@flatMapLatest emptyFlow()
                projectRepository.getNotesPaged(
                    project,
                    SearchQuery(tagFilters = listOf("quick-note"), sortBy = SortBy.CREATED_AT),
                    includeText = true,
                    includeFrontMatter = false,
                )
            }
            .cachedIn(viewModelScope)

        fun onMessengerOpened(project: Project, afterUpdate: () -> Unit = {}) {
            viewModelScope.launch {
                projectRepository.syncDatabase(project)
                val pinnedNotes = projectRepository.getNotes(
                    project = project,
                    SearchQuery(
                        tagFilters = listOf("quick-note", "pinned"),
                        sortBy = SortBy.CREATED_AT,
                    ),
                    includeText = true,
                    includeFrontMatter = false,
                )
                _uiState.update {
                    it.copy(
                        messengerPinnedNotes = pinnedNotes,
                        messengerIsLoading = false,
                    )
                }
                afterUpdate()
            }
        }

        fun onNewNoteTextChanged(text: String) {
            _uiState.update { it.copy(messengerNewNoteText = text) }
        }

        fun startEditNote(note: Note, parsedText: String) {
            _uiState.update {
                it.copy(
                    messengerEditingNote = note,
                    messengerNewNoteText = parsedText,
                )
            }
        }

        fun cancelEditNote() {
            _uiState.update {
                it.copy(messengerEditingNote = null, messengerNewNoteText = "")
            }
        }

        fun onSaveEditedNote(
            attachments: List<Attachment> = emptyList(),
            afterUpdate: () -> Unit = {},
        ) {
            val project = _uiState.value.project ?: return
            val note = _uiState.value.messengerEditingNote ?: return
            val newBodyText = _uiState.value.messengerNewNoteText.trim()

            viewModelScope.launch(Dispatchers.IO) {
                val fullText = noteRepository.getNoteText(note, includeFrontMatter = true)
                val frontMatterEnd = run {
                    if (!fullText.trimStart().startsWith("---")) return@run 0
                    val lines = fullText.lines()
                    val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
                    if (closeIdx < 0) 0
                    else lines.take(closeIdx + 2).joinToString("\n").length
                }
                val frontMatter = fullText.substring(0, frontMatterEnd).trimEnd()

                val attachmentLines = buildString {
                    attachments.forEach { attachment ->
                        when (attachment.type) {
                            AttachmentType.PENDING_IMAGE -> {
                                val path = projectRepository.copyToAssets(project, attachment.uri)
                                append("\n![image](<$path>)")
                            }

                            AttachmentType.PENDING_FILE -> {
                                val path = projectRepository.copyToAssets(project, attachment.uri)
                                val label = attachment.displayName
                                append("\n[$label](<$path>)")
                            }

                            AttachmentType.IMAGE -> append("\n![image](<${attachment.path}>)")
                            AttachmentType.FILE -> append("\n[${attachment.displayName}](<${attachment.path}>)")
                        }
                    }
                }
                val newFullContent = when {
                    newBodyText.isNotEmpty() && attachmentLines.isNotEmpty() ->
                        "$frontMatter\n\n$newBodyText$attachmentLines"

                    newBodyText.isNotEmpty() ->
                        "$frontMatter\n\n$newBodyText"

                    attachmentLines.isNotEmpty() ->
                        "$frontMatter\n$attachmentLines"

                    else -> frontMatter
                }

                noteRepository.saveNoteText(note, newFullContent)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            messengerNewNoteText = "",
                            messengerEditingNote = null,
                        )
                    }
                }
                updateNoteLists(afterUpdateMessenger = afterUpdate)
            }
        }

        fun onSendNote(
            attachments: List<Attachment> = emptyList(),
            afterUpdate: () -> Unit = {},
        ) {
            val project = _uiState.value.project ?: return
            val text = _uiState.value.messengerNewNoteText.trim()
            if (text.isBlank() && attachments.isEmpty()) return

            val timestamp = DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
            val name = "quick-note-$timestamp"
            val tags = listOf("quick-note")

            viewModelScope.launch(Dispatchers.IO) {
                val uri = noteRepository.createNote(project, name, tags)
                if (uri != null) {
                    val newNote = noteRepository.getNoteByUri(uri)
                    val currentContent =
                        noteRepository.getNoteText(newNote, includeFrontMatter = true)

                    val attachmentLines = buildString {
                        attachments.forEach { attachment ->
                            when (attachment.type) {
                                AttachmentType.PENDING_IMAGE -> {
                                    val path =
                                        projectRepository.copyToAssets(project, attachment.uri)
                                    append("\n![image](<$path>)")
                                }

                                AttachmentType.PENDING_FILE -> {
                                    val path =
                                        projectRepository.copyToAssets(project, attachment.uri)
                                    val label =
                                        attachment.displayName
                                    append("\n[$label](<$path>)")
                                }

                                else -> {}
                            }
                        }
                    }

                    val fullContent = when {
                        text.isNotEmpty() && attachmentLines.isNotEmpty() ->
                            "$currentContent\n\n$text$attachmentLines"

                        text.isNotEmpty() ->
                            "$currentContent\n\n$text"

                        else ->
                            "$currentContent\n$attachmentLines"
                    }

                    noteRepository.saveNoteText(newNote, fullContent)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(messengerNewNoteText = "") }
                    }
                    updateNoteLists(afterUpdateMessenger = afterUpdate)
                }
            }
        }

        fun ensureLinkPreview(url: String) {
            if (_uiState.value.messengerLinkPreviews.containsKey(url)) return
            _uiState.update { it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to null)) }

            viewModelScope.launch {
                val preview = linkPreviewRepository.getLinkPreview(url)
                if (preview != null) {
                    _uiState.update {
                        it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to preview))
                    }
                    return@launch
                }
            }
        }

        fun toggleNoteSelection(uriString: String) {
            _uiState.update {
                val sel = it.messengerSelectedNotes
                it.copy(messengerSelectedNotes = if (sel.contains(uriString)) sel - uriString else sel + uriString)
            }
        }

        fun clearSelection() {
            _uiState.update { it.copy(messengerSelectedNotes = emptySet()) }
        }

        fun deleteSelectedNotes() {
            val uris = _uiState.value.messengerSelectedNotes
            val project = _uiState.value.project ?: return
            viewModelScope.launch(Dispatchers.IO) {
                uris.forEach { u ->
                    runCatching { noteRepository.deleteNote(noteRepository.getNoteByUri(u.toUri())) }
                }
                projectRepository.syncDatabase(project)
                withContext(Dispatchers.Main) {
                    clearSelection()
                    updateNoteLists()
                }
            }
        }

        suspend fun getSelectedNotesText(): String = withContext(Dispatchers.IO) {
            _uiState.value.messengerSelectedNotes.mapNotNull { u ->
                runCatching {
                    val note = noteRepository.getNoteByUri(u.toUri())
                    val text = noteRepository.getNoteText(note, includeFrontMatter = false)

                    MarkdownParser.stripAttachments(text, MarkdownParser.parse(text))
                }.getOrNull()?.takeIf { it.isNotBlank() }
            }.joinToString("\n\n")
        }
    }


    fun updateNoteLists(afterUpdateSearch: () -> Unit = {}, afterUpdateMessenger: () -> Unit = {}) {
        val project = _uiState.value.project ?: return
        navigation.onSearchQueryChanged(afterUpdate = afterUpdateSearch)
        messenger.onMessengerOpened(project, afterUpdate = afterUpdateMessenger)
    }

    fun onShareIntent(text: String?, attachments: List<Attachment>) {
        _uiState.update { state ->
            state.copy(
                messengerNewNoteText = if (!text.isNullOrEmpty()) text else state.messengerNewNoteText,
                pendingIntentAttachments = attachments,
            )
        }
    }

    fun consumePendingIntentAttachments(): List<Attachment> {
        val pending = _uiState.value.pendingIntentAttachments
        if (pending.isNotEmpty()) {
            _uiState.update { it.copy(pendingIntentAttachments = emptyList()) }
        }
        return pending
    }

}