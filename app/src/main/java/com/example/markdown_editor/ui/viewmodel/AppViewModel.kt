package com.example.markdown_editor.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.domain.parser.MarkdownParser
import com.example.markdown_editor.ui.viewmodel.AppUiState
import com.example.markdown_editor.ui.viewmodel.EditorEvent
import com.example.markdown_editor.ui.editor.MarkdownAnnotator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepositoryImpl(
        context = application,
    )

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        loadSavedProject()
    }

    // Called with the URI returned by the system folder picker
    fun onProjectSelected(uri: Uri) {
        viewModelScope.launch {
            val name = uri.lastPathSegment?.substringAfterLast(":") ?: "Project"
            val project = repository.buildProject(uri, name)
            repository.saveProject(project)
            _uiState.update { it.copy(project = project, isLoadingNotes = true) }
            loadNotes(project)
        }
    }

    fun onNoteSelected(note: Note) {
        _uiState.update { it.copy(activeNoteUri = note.uri) }
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
                loadNotes(project)
            }
        }
    }

    private var searchJob: Job? = null
    fun onSearchQueryChanged(raw: String) {
        _uiState.update { it.copy(searchQuery = raw) }

        val project = _uiState.value.project ?: run {
            _uiState.update { it.copy(searchResults = null) }
            return
        }

        val parsed = SearchQuery.Companion.parse(raw.trim())
        if (parsed.isEmpty) {
            searchJob?.cancel()
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = repository.searchNotes(project, parsed)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }


    fun editorOnEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.InsertSyntax -> editorInsertSyntax(event.syntax, event.cursorOffset)
        }
    }

    private fun editorInsertSyntax(syntax: String, cursorOffset: Int) {
        val current = _uiState.value.editorTextFieldValue
        val cursor = current.selection.start
        val newText = current.text.substring(0, cursor) + syntax + current.text.substring(cursor)
        val newCursor = cursor + cursorOffset
        val newValue = current.copy(
            text = newText,  // TextFieldValue accepts plain text here
            selection = TextRange(newCursor)
        )
        editorOnContentChanged(newValue)
    }

    fun editorOnContentChanged(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.editorTextFieldValue.text

        if (!textChanged) {
            _uiState.update { it.copy(editorTextFieldValue = newValue) }
            return
        }

        // Compute spans synchronously so textFieldValue and annotatedString
        // are always updated in the same state emission — no gap, no flash.
        val spans = MarkdownParser.parse(newValue.text)
        val annotated = MarkdownAnnotator.annotate(newValue.text, spans)

        _uiState.update {
            it.copy(
                editorTextFieldValue = newValue,
                editorAnnotatedString = annotated
            )
        }
    }

    fun editorOnNoteOpened(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getNoteByUri(uri)
            val text = repository.getNoteText(note)
            withContext(Dispatchers.Main) {
                editorOnContentChanged(TextFieldValue(text))
            }
            // Track URI so save knows where to write
            _uiState.update { it.copy(editorNote = note) }
        }
    }

    fun editorOnSave() {
        val note = _uiState.value.editorNote ?: return
        val text = _uiState.value.editorTextFieldValue.text
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNoteText(note, text)
        }
    }

    private fun loadSavedProject() {
        viewModelScope.launch {
            val project = repository.loadSavedProject()
            if (project != null) {
                _uiState.update { it.copy(project = project, isLoadingNotes = true) }
                loadNotes(project)
            }
        }
    }

    private fun loadNotes(project: Project) {
        viewModelScope.launch {
            repository.getNotes(project).collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoadingNotes = false) }
            }
        }
    }
}