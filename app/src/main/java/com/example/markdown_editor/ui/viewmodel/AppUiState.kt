package com.example.markdown_editor.ui.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.model.SpanInfo

data class AppUiState(
    val project: Project? = null,
    val notes: List<Note> = emptyList(),
    val isLoadingNotes: Boolean = false,
    val error: String? = null,
    val activeNote: Note? = null,

    val isCreateNoteDialogVisible: Boolean = false,
    val newNoteNameInput: String = "",

    val searchQuery: String = "",
    val searchResults: List<Note>? = null,   // null = not searching, empty list = no results
    val isSearching: Boolean = false,

    val editorTextFieldValue: TextFieldValue = TextFieldValue(),
    val editorAnnotatedString: AnnotatedString = androidx.compose.ui.text.AnnotatedString(""),
    val editorSpans: List<SpanInfo> = emptyList(),

    val messengerNotesList: List<Note> = emptyList(),
    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
)