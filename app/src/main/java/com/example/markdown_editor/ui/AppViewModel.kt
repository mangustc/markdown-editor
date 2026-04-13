package com.example.markdown_editor.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun onNoteSelected(note: com.example.markdown_editor.data.model.Note) {
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

        val parsed = SearchQuery.parse(raw.trim())
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
    private fun loadSavedProject() {
        viewModelScope.launch {
            val project = repository.loadSavedProject()
            if (project != null) {
                _uiState.update { it.copy(project = project, isLoadingNotes = true) }
                loadNotes(project)
            }
        }
    }

    private fun loadNotes(project: com.example.markdown_editor.data.model.Project) {
        viewModelScope.launch {
            repository.getNotes(project).collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoadingNotes = false) }
            }
        }
    }
}