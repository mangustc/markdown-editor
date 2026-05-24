package com.example.markdown_editor.domain.messenger

import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.markdown.SpanInfo

private val URL_PATTERN = Regex("""https?://[^\s<>"')]+""")

data class ParsedNoteBody(
    val text: String,
    val attachments: List<Attachment>,
    val links: Sequence<MatchResult>,
) {
    companion object {
        fun parse(body: String, project: Project): ParsedNoteBody {
            val spans = MarkdownParser.parse(body)

            val imageAttachments = spans.filterIsInstance<SpanInfo.Image>().map { span ->
                Attachment(
                    uri = project.getFileUri(span.payload),
                    displayName = span.payload,
                    path = span.payload,
                    type = AttachmentType.IMAGE,
                )
            }

            val fileAttachments = spans.filterIsInstance<SpanInfo.Link>().mapNotNull { span ->
                if (span.linkType != SpanInfo.Link.LinkType.FILE) return@mapNotNull null
                Attachment(
                    uri = project.getFileUri(span.payload),
                    displayName = span.label,
                    path = span.payload,
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
