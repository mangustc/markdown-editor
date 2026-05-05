package com.example.markdown_editor.domain.messenger

import android.net.Uri


enum class AttachmentType { PENDING_IMAGE, PENDING_FILE, IMAGE, FILE }

data class Attachment(
    val uri: Uri,
    val displayName: String,
    val path: String? = null,
    val type: AttachmentType,
)
