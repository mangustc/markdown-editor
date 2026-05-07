package com.example.markdown_editor.domain.markdown

import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

object MarkdownParser {
    private val parser: Parser = Parser.builder()
        .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
        .build()

    fun parse(text: String): List<SpanInfo> {
        if (text.isEmpty()) return emptyList()

        val lineOffsets = buildLineOffsets(text)
        val document = parser.parse(text)
        val spans = mutableListOf<SpanInfo>()

        document.accept(
            object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    val type = when (heading.level) {
                        1 -> TokenType.H1
                        2 -> TokenType.H2
                        else -> TokenType.H3
                    }
                    heading.spanInfo(lineOffsets, type)?.let { spans.add(it) }
                    visitChildren(heading)
                }

                override fun visit(strongEmphasis: StrongEmphasis) {
                    strongEmphasis.spanInfo(lineOffsets, TokenType.BOLD)?.let { spans.add(it) }
                    visitChildren(strongEmphasis)
                }

                override fun visit(emphasis: Emphasis) {
                    emphasis.spanInfo(lineOffsets, TokenType.ITALIC)?.let { spans.add(it) }
                    visitChildren(emphasis)
                }

                override fun visit(code: Code) {
                    code.spanInfo(lineOffsets, TokenType.CODE_INLINE)?.let { spans.add(it) }
                }

                override fun visit(fencedCodeBlock: FencedCodeBlock) {
                    fencedCodeBlock.spanInfo(lineOffsets, TokenType.CODE_BLOCK)
                        ?.let { spans.add(it) }
                }

                override fun visit(indentedCodeBlock: IndentedCodeBlock) {
                    indentedCodeBlock.spanInfo(lineOffsets, TokenType.CODE_BLOCK)
                        ?.let { spans.add(it) }
                }

                override fun visit(image: Image) {
                    image.spanInfo(lineOffsets, TokenType.IMAGE, image.destination)
                        ?.let { spans.add(it) }
                }

                override fun visit(link: Link) {
                    val label = (link.firstChild as? Text)?.literal ?: link.destination
                    val type =
                        if (link.destination.startsWith("http")) TokenType.LINK else TokenType.FILE
                    link.spanInfo(lineOffsets, type, link.destination, label)?.let { spans.add(it) }
                    visitChildren(link)
                }

                override fun visit(listItem: ListItem) {
                    listItem.spanInfo(lineOffsets, TokenType.LIST_ITEM)?.let { spans.add(it) }
                    visitChildren(listItem)
                }

                override fun visit(blockQuote: BlockQuote) {
                    blockQuote.spanInfo(lineOffsets, TokenType.BLOCKQUOTE)?.let { spans.add(it) }
                    visitChildren(blockQuote)
                }
            },
        )
        return spans
    }

    fun stripAttachments(text: String, spans: List<SpanInfo>): String {
        val toRemove = spans.filter { it.type == TokenType.IMAGE || it.type == TokenType.FILE }
            .sortedByDescending { it.start }
        var res = text
        for (span in toRemove) {
            if (span.start < span.end && span.end <= res.length) {
                res = res.removeRange(span.start, span.end)
            }
        }
        return res.trim()
    }

    private fun buildLineOffsets(text: String): IntArray {
        val offsets = ArrayList<Int>(text.count { it == '\n' } + 1)
        offsets.add(0)
        text.forEachIndexed { i, c -> if (c == '\n') offsets.add(i + 1) }
        return offsets.toIntArray()
    }

    private fun Node.spanInfo(
        lineOffsets: IntArray,
        type: TokenType,
        payload: String? = null,
        label: String? = null,
    ): SpanInfo? {
        val srcSpans = sourceSpans
        if (srcSpans.isEmpty()) return null

        val first = srcSpans.first()
        val last = srcSpans.last()

        val start = lineOffsets.getOrNull(first.lineIndex)?.plus(first.columnIndex) ?: return null
        val end = lineOffsets.getOrNull(last.lineIndex)?.plus(last.columnIndex + last.length)
            ?: return null

        if (start >= end) return null
        return SpanInfo(start, end, type, payload, label)
    }
}