package com.example.markdown_editor.ui.messenger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class MessengerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepositoryImpl(
        context = application,
    )

    private val _uiState = MutableStateFlow(MessengerUiState())
    val uiState: StateFlow<MessengerUiState> = _uiState.asStateFlow()

    fun onMessengerOpened(project: Project) {
        viewModelScope.launch {
            _uiState.update { it.copy(project = project) }

            val notes = repository.searchNotes(project, SearchQuery(tagFilters = listOf("quick-note"))).map { note ->
                try {
                    val textContent = repository.getNoteText(note, includeFrontMatter = false)
                    note.copy(text = textContent)
                } catch (e: Exception) {
                    note
                }
            }

            _uiState.update {
                it.copy(notesList = notes)
            }
        }
    }

    fun onNoteClicked(note: Note) {
        // When user clicks note in messenger feed, switch to EditorScreen
        // This requires AppViewModel/AppScaffold coordination, but for VM logic:
        // Emit event or callback to trigger navigation to EditorScreen with this note's URI.
    }

    fun onCreateNote() {
        val project = _uiState.value.project ?: return
        val name = "quick-note-${java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")}"
        val tags = listOf("quick-note")

        viewModelScope.launch(Dispatchers.IO) {
            repository.createNote(project, name, tags)
                ?.let { _ ->
                    onMessengerOpened(project)
                }
        }
    }
}

