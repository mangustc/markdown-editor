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

    val editorTextFieldValue: TextFieldValue = TextFieldValue(),
    val editorAnnotatedString: AnnotatedString = AnnotatedString(""),
    val editorSpans: List<SpanInfo> = emptyList(),

    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
    val messengerLinkPreviews: Map<String, LinkPreview?> = emptyMap(),
    val messengerEditingNote: Note? = null,

    val pendingIntentAttachments: List<Attachment> = emptyList(),
)