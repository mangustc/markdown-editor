package com.example.markdown_editor.domain.messenger

enum class AttachmentType { PENDING_IMAGE, PENDING_FILE, IMAGE, FILE }

data class Attachment(
    val path: String,
    val displayName: String,
    val relativePath: String? = null,
    val type: AttachmentType,
)
