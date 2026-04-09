package com.example.markdown_editor.ui

import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project

data class AppUiState(
    val project: Project? = null,
    val notes: List<Note> = emptyList(),
    val activeNoteUri: android.net.Uri? = null,
    val isLoadingNotes: Boolean = false,
    val error: String? = null,

    val isCreateNoteDialogVisible: Boolean = false,
    val newNoteNameInput: String = "",
)