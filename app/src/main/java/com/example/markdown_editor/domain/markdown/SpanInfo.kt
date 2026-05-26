package com.example.markdown_editor.domain.markdown

import java.net.URI

sealed interface SpanInfo {
    data class TextRange(
        val start: Int,
        val end: Int,
    )

    val range: TextRange

    data class Heading(override val range: TextRange, val level: Int) : SpanInfo
    data class Bold(override val range: TextRange) : SpanInfo
    data class Italic(override val range: TextRange) : SpanInfo
    data class CodeInline(override val range: TextRange) : SpanInfo
    data class CodeBlock(override val range: TextRange) : SpanInfo
    data class Image(override val range: TextRange, val payload: String) : SpanInfo
    data class Link(
        override val range: TextRange,
        val payload: String,
        val label: String,
        val payloadRange: TextRange,
        val labelRange: TextRange,
    ) : SpanInfo {
        enum class LinkType { FILE, NOTE, HTTP }

        val linkType: LinkType
            get() {
                val isHttp = (payload.startsWith("http://", ignoreCase = true) ||
                        payload.startsWith("https://", ignoreCase = true)) &&
                        runCatching { URI(payload) }.isSuccess
                if (isHttp) return LinkType.HTTP
                if (payload.endsWith(".md", ignoreCase = true)) return LinkType.NOTE
                return LinkType.FILE
            }
    }

    data class ListItem(override val range: TextRange) : SpanInfo
    data class Blockquote(override val range: TextRange) : SpanInfo
}