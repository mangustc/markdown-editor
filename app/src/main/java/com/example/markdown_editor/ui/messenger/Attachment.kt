package com.example.markdown_editor.ui.messenger

import android.net.Uri

sealed class Attachment {
    data class PendingPhoto(val uri: Uri) : Attachment()
    data class PendingAttachedFile(val uri: Uri, val displayName: String?) : Attachment()
    data class Photo(val path: String) : Attachment()
    data class AttachedFile(val path: String, val displayName: String) : Attachment()
}
 