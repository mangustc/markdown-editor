package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.data.model.FrontMatter
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.messenger.Attachment

data class AppUiState(
    val project: Project? = null,
    val error: String? = null,
    val activeNote: Note? = null,
    val allProjectTags: List<String> = emptyList(),

    val isCreateNoteDialogVisible: Boolean = false,
    val newNoteNameInput: String = "",

    val isNoteShowInfoDialogVisible: Boolean = false,
    val isNoteDeleteDialogVisible: Boolean = false,
    val isNoteRenameDialogVisible: Boolean = false,
    val noteRenameInput: String = "",
    val dialogNote: Note? = null,

    val editorFrontMatter: FrontMatter? = null,
    val editorCanUndo: Boolean = false,
    val editorCanRedo: Boolean = false,
    val editorVersion: Int = 0,
    val editorSavedVersion: Int = 0,

    val messengerIsLoading: Boolean = true,
    val messengerNewNoteText: String = "",
    val messengerLinkPreviews: Map<String, LinkPreview?> = emptyMap(),
    val messengerEditingNote: Note? = null,
    val messengerPinnedNotes: List<Note> = emptyList(),
    val messengerSelectedNotes: Set<String> = emptySet(),

    val pendingIntentAttachments: List<Attachment> = emptyList(),
)