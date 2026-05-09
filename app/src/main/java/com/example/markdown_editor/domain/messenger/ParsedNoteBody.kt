package com.example.markdown_editor.domain.messenger

import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.model.TokenType

private val URL_PATTERN = Regex("""https?://[^\s<>"')]+""")

data class ParsedNoteBody(
    val text: String,
    val attachments: List<Attachment>,
    val links: Sequence<MatchResult>,
) {
    companion object {
        fun parse(body: String, project: Project): ParsedNoteBody {
            val spans = MarkdownParser.parse(body)

            val imageAttachments = spans.filter { it.type == TokenType.IMAGE }.map { span ->
                val path = span.payload ?: ""
                Attachment(
                    uri = project.getFileUri(path),
                    displayName = path,
                    path = path,
                    type = AttachmentType.IMAGE,
                )
            }

            val fileAttachments = spans.filter { it.type == TokenType.FILE }.map { span ->
                val path = span.payload ?: ""
                val label = span.label ?: path
                Attachment(
                    uri = project.getFileUri(path),
                    displayName = label,
                    path = path,
                    type = AttachmentType.FILE,
                )
            }

            val text = MarkdownParser.stripAttachments(body, spans).ifBlank { "" }

            val links = URL_PATTERN.findAll(text)

            return ParsedNoteBody(
                text = text,
                attachments = imageAttachments + fileAttachments,
                links = links,
            )
        }
    }
}


