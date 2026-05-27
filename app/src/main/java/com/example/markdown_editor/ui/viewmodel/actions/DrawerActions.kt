package com.example.markdown_editor.ui.viewmodel.actions

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.ui.viewmodel.AppDeps
import com.example.markdown_editor.ui.viewmodel.events.NavigationEvent
import com.example.markdown_editor.ui.viewmodel.events.SearchEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DrawerActions(
    private val deps: AppDeps,
) {
    val searchState = TextFieldState()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResultsPaged: Flow<PagingData<Note>> = combine(
        deps.uiState.map { it.project }.distinctUntilChanged(),
        snapshotFlow { searchState.text },
    ) { project, text -> project to text.toString() }
        .flatMapLatest { (project, queryStr) ->
            if (project == null) return@flatMapLatest emptyFlow()
            deps.projectRepo.syncDatabase(project)
            val parsedInit = SearchQuery.parse(queryStr.trim())
            val parsed = parsedInit.copy(
                negatedTagFilters = parsedInit.negatedTagFilters + "quick-note",
                pinnedFirst = true,
            )
            deps.projectRepo.getNotesPaged(project, parsed)
        }
        .cachedIn(deps.scope)

    fun onSearchEvent(event: SearchEvent) {
        event.execute(searchState = searchState)
    }

    fun onNoteSelected(note: Note) {
        deps.globalActions.onEvent(NavigationEvent.CloseDrawer)
        deps.globalActions.onEvent(NavigationEvent.GoToEditor(note = note))
    }

    fun showCreateNoteDialog() {
        deps.uiState.update { it.copy(isCreateNoteDialogVisible = true) }
    }

    fun dismissCreateNoteDialog() {
        deps.uiState.update { it.copy(isCreateNoteDialogVisible = false, newNoteNameInput = "") }
    }

    fun updateNewNoteName(name: String) {
        deps.uiState.update { it.copy(newNoteNameInput = name) }
    }

    fun onCreateNote() {
        val project = deps.uiState.value.project ?: return
        val nameToUse = deps.uiState.value.newNoteNameInput
        deps.scope.launch {
            val uri = deps.noteRepo.createNote(project, nameToUse)
            if (uri != null) {
                deps.projectRepo.syncDatabase(project)
                deps.globalActions.updateNoteLists()
            }
        }
    }

    fun showNoteDeleteDialog(note: Note) {
        deps.uiState.update { it.copy(isNoteDeleteDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteDeleteDialog() {
        deps.uiState.update { it.copy(isNoteDeleteDialogVisible = false, dialogNote = null) }
    }

    fun onDeleteNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val activeNote = deps.uiState.value.activeNote
            deps.noteRepo.deleteNote(note)
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
            if (note.projectFile.relativePath == activeNote?.projectFile?.relativePath)
                deps.globalActions.onEvent(NavigationEvent.GoBack)
        }
    }

    fun showNoteShowInfoDialog(note: Note) {
        deps.uiState.update { it.copy(isNoteShowInfoDialogVisible = true, dialogNote = note) }
    }

    fun dismissNoteShowInfoDialog() {
        deps.uiState.update { it.copy(isNoteShowInfoDialogVisible = false, dialogNote = null) }
    }

    fun showNoteRenameDialog(note: Note) {
        deps.uiState.update {
            it.copy(
                isNoteRenameDialogVisible = true,
                noteRenameInput = note.name,
                dialogNote = note,
            )
        }
    }

    fun dismissNoteRenameDialog() {
        deps.uiState.update {
            it.copy(isNoteRenameDialogVisible = false, noteRenameInput = "", dialogNote = null)
        }
    }

    fun onRenameNameInputChanged(newName: String) {
        deps.uiState.update { it.copy(noteRenameInput = newName) }
    }

    fun onRenameNote(note: Note, newName: String) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            deps.noteRepo.renameNote(note, newName)
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
        }
    }

    fun onPinNote(note: Note) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            deps.noteRepo.toggleNotePin(note)
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
        }
    }
}