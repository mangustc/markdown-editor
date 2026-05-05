package com.example.markdown_editor.domain.editor

import android.net.Uri

sealed class EditorEvent {
    data class InsertSyntax(val syntax: String, val cursorOffset: Int) : EditorEvent()
    data class AttachPhoto(val uri: Uri) : EditorEvent()
    data class AttachFile(
        val uri: Uri,
        val displayName: String? = null,
    ) : EditorEvent()

    object Undo : EditorEvent()
    object Redo : EditorEvent()
}