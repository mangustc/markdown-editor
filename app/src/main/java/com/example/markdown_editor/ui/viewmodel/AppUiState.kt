package com.example.markdown_editor.ui.viewmodel

import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.LinkPreview
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings

data class AppUiState(
    val project: Project? = null,
    val error: String? = null,
    val activeNote: Note? = null,
    val allProjectTags: List<String> = emptyList(),
    val isSyncInProgress: Boolean = false,
    val settings: Settings? = null,

    val isCreateNoteDialogVisible: Boolean = false,
    val newNoteNameInput: String = "",

    val isNoteShowInfoDialogVisible: Boolean = false,
    val isNoteDeleteDialogVisible: Boolean = false,
    val isNoteRenameDialogVisible: Boolean = false,
    val noteRenameInput: String = "",
    val dialogNote: Note? = null,

    val isLinkNoteDialogVisible: Boolean = false,
    val editorFrontMatter: FrontMatter? = null,
    val editorCanUndo: Boolean = false,
    val editorCanRedo: Boolean = false,
    val editorVersion: Int = 0,
    val editorSavedVersion: Int = 0,
    val isViewingMode: Boolean = true,

    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
    val messengerLinkPreviews: Map<String, LinkPreview?> = emptyMap(),
    val messengerEditingNote: Note? = null,
    val messengerPinnedMessages: List<MessageBody> = emptyList(),
    val messengerSelectedNotes: Set<String> = emptySet(),

    val isSettingsDialogVisible: Boolean = false,

    val pendingIntentAttachments: List<Attachment> = emptyList(),
)