package com.example.markdown_editor.ui.messenger

import android.net.Uri
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project

data class MessengerUiState(
    val notesList: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val project: Project? = null,

    val newNoteText: String = "",
)