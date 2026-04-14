package com.example.markdown_editor.ui.editor

import android.net.Uri

sealed class EditorEvent {
    data class InsertSyntax(val syntax: String, val cursorOffset: Int) : EditorEvent()
    data class AttachPhoto(val uri: Uri, val mimeType: String?) : EditorEvent()
    data class AttachFile(
        val uri: Uri,
        val mimeType: String?,
        val displayName: String?
    ) : EditorEvent()
}