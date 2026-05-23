package com.example.markdown_editor.domain.model

sealed interface SpanInfo {
    val start: Int
    val end: Int

    data class Heading(override val start: Int, override val end: Int, val level: Int) : SpanInfo
    data class Bold(override val start: Int, override val end: Int) : SpanInfo
    data class Italic(override val start: Int, override val end: Int) : SpanInfo
    data class CodeInline(override val start: Int, override val end: Int) : SpanInfo
    data class CodeBlock(override val start: Int, override val end: Int) : SpanInfo
    data class Image(override val start: Int, override val end: Int, val payload: String) : SpanInfo
    data class File(
        override val start: Int,
        override val end: Int,
        val payload: String,
        val label: String,
    ) : SpanInfo

    data class Link(
        override val start: Int,
        override val end: Int,
        val payload: String,
        val label: String,
    ) : SpanInfo

    data class ListItem(override val start: Int, override val end: Int) : SpanInfo
    data class Blockquote(override val start: Int, override val end: Int) : SpanInfo
}