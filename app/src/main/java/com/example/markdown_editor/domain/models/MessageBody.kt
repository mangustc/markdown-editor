package com.example.markdown_editor.domain.models

data class MessageBody(
    val text: String,
    val attachments: List<Attachment>,
    val links: Sequence<MatchResult>,
    val note: Note,
)
