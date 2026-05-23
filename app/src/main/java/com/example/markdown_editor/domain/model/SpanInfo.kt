package com.example.markdown_editor.domain.model

import java.net.URI

sealed interface SpanInfo {
    val start: Int
    val end: Int

    data class Heading(override val start: Int, override val end: Int, val level: Int) : SpanInfo
    data class Bold(override val start: Int, override val end: Int) : SpanInfo
    data class Italic(override val start: Int, override val end: Int) : SpanInfo
    data class CodeInline(override val start: Int, override val end: Int) : SpanInfo
    data class CodeBlock(override val start: Int, override val end: Int) : SpanInfo
    data class Image(override val start: Int, override val end: Int, val payload: String) : SpanInfo
    data class Link(
        override val start: Int,
        override val end: Int,
        val payload: String,
        val label: String,
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

    data class ListItem(override val start: Int, override val end: Int) : SpanInfo
    data class Blockquote(override val start: Int, override val end: Int) : SpanInfo
}