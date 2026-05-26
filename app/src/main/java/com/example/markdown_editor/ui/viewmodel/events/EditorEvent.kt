package com.example.markdown_editor.ui.viewmodel.events

import android.net.Uri

sealed interface EditorEvent {
    data class InsertSyntax(val syntax: String, val cursorOffset: Int) : EditorEvent
    data class AttachPhoto(val uri: Uri) : EditorEvent
    data class AttachFile(
        val uri: Uri,
        val displayName: String? = null,
    ) : EditorEvent

    data object Undo : EditorEvent
    data object Redo : EditorEvent
}