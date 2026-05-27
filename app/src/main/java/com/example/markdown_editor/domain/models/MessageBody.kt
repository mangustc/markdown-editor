package com.example.markdown_editor.domain.models

import com.example.markdown_editor.domain.markdown.MarkdownParser

data class MessageBody(
    val text: String,
    val attachments: List<Attachment>,
    val links: Sequence<MatchResult>,
    val note: Note,
) {
    companion object {
        private val URL_PATTERN = Regex("""https?://[^\s<>"')]+""")

        suspend fun parse(
            note: Note,
            getProjectFile: suspend (RelativePath) -> ProjectFile?,
        ): MessageBody {
            val body = note.body ?: ""
            val spans = MarkdownParser.parse(body)

            val imageAttachments = spans.filterIsInstance<SpanInfo.Image>().mapNotNull { span ->
                val projectFile = getProjectFile(RelativePath(span.payload))
                    ?: return@mapNotNull null
                Attachment.ProjectAttachment(
                    type = Attachment.AttachmentType.IMAGE,
                    fileSystemPath = projectFile.fileSystemPath,
                    relativePath = projectFile.relativePath,
                    displayName = span.payload,
                )
            }

            val fileAttachments = spans.filterIsInstance<SpanInfo.Link>().mapNotNull { span ->
                val projectFile = getProjectFile(RelativePath(span.payload))
                    ?: return@mapNotNull null
                Attachment.ProjectAttachment(
                    type = Attachment.AttachmentType.FILE,
                    fileSystemPath = projectFile.fileSystemPath,
                    relativePath = projectFile.relativePath,
                    displayName = span.payload,
                )
            }

            val text = MarkdownParser.stripAttachments(body, spans).ifBlank { "" }
            val links = URL_PATTERN.findAll(text)

            return MessageBody(
                text = text,
                attachments = imageAttachments + fileAttachments,
                links = links,
                note = note,
            )
        }
    }
}
