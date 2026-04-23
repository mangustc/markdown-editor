package com.example.markdown_editor.ui.messenger

import android.net.Uri

sealed class Attachment {
    data class PendingPhoto(val uri: Uri) : Attachment()
    data class PendingAttachedFile(val uri: Uri, val displayName: String?) : Attachment()
    data class Photo(val uri: Uri) : Attachment()
    data class AttachedFile(val uri: Uri, val displayName: String) : Attachment()
}
 