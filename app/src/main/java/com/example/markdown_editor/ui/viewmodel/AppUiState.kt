package com.example.markdown_editor.ui.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.ui.messenger.Attachment

data class AppUiState(
    val project: Project? = null,
    val error: String? = null,
    val activeNote: Note? = null,

    val isCreateNoteDialogVisible: Boolean = false,
    val newNoteNameInput: String = "",

    val isNoteShowInfoDialogVisible: Boolean = false,
    val isNoteDeleteDialogVisible: Boolean = false,
    val isNoteRenameDialogVisible: Boolean = false,
    val noteRenameInput: String = "",
    val dialogNote: Note? = null,

    val searchQuery: String = "",
    val searchResults: List<Note> = emptyList(),

    val editorTextFieldValue: TextFieldValue = TextFieldValue(),
    val editorAnnotatedString: AnnotatedString = androidx.compose.ui.text.AnnotatedString(""),
    val editorSpans: List<SpanInfo> = emptyList(),

    val messengerNotesList: List<Note> = emptyList(),
    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
    val messengerLinkPreviews: Map<String, LinkPreview?> = emptyMap(),

    val pendingIntentAttachments: List<Attachment> = emptyList(),
)