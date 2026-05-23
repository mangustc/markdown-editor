package com.example.markdown_editor.domain.markdown

import com.example.markdown_editor.domain.model.SpanInfo
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

        var frontMatterEnd = 0
        if (text.startsWith("---")) {
            val firstLineEnd = text.indexOf('\n')
            if (firstLineEnd != -1) {
                val secondDash = text.indexOf("---", firstLineEnd + 1)
                if (secondDash != -1) {
                    frontMatterEnd = secondDash + 3
                }
            }
        }

        val lineOffsets = buildLineOffsets(text)
        val document = parser.parse(text)
        val spans = mutableListOf<SpanInfo>()

        document.accept(
            object : AbstractVisitor() {
                override fun visit(heading: Heading) {
                    heading.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.Heading(s, e, heading.level)) }
                    visitChildren(heading)
                }

                override fun visit(strongEmphasis: StrongEmphasis) {
                    strongEmphasis.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.Bold(s, e)) }
                    visitChildren(strongEmphasis)
                }

                override fun visit(emphasis: Emphasis) {
                    emphasis.bounds(lineOffsets)?.let { (s, e) -> spans.add(SpanInfo.Italic(s, e)) }
                    visitChildren(emphasis)
                }

                override fun visit(code: Code) {
                    code.bounds(lineOffsets)?.let { (s, e) -> spans.add(SpanInfo.CodeInline(s, e)) }
                }

                override fun visit(fencedCodeBlock: FencedCodeBlock) {
                    fencedCodeBlock.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.CodeBlock(s, e)) }
                }

                override fun visit(indentedCodeBlock: IndentedCodeBlock) {
                    indentedCodeBlock.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.CodeBlock(s, e)) }
                }

                override fun visit(image: Image) {
                    image.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.Image(s, e, image.destination)) }
                }

                override fun visit(link: Link) {
                    val label = (link.firstChild as? Text)?.literal ?: link.destination
                    link.bounds(lineOffsets)?.let { (s, e) ->
                        spans.add(SpanInfo.Link(s, e, link.destination, label))
                    }
                    visitChildren(link)
                }

                override fun visit(listItem: ListItem) {
                    listItem.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.ListItem(s, e)) }
                    visitChildren(listItem)
                }

                override fun visit(blockQuote: BlockQuote) {
                    blockQuote.bounds(lineOffsets)
                        ?.let { (s, e) -> spans.add(SpanInfo.Blockquote(s, e)) }
                    visitChildren(blockQuote)
                }
            },
        )
        return spans.filter { it.start >= frontMatterEnd }
    }

    fun stripAttachments(text: String, spans: List<SpanInfo>): String {
        val toRemove = spans.filter { it is SpanInfo.Image || it is SpanInfo.Link }
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

    private fun Node.bounds(lineOffsets: IntArray): Pair<Int, Int>? {
        val srcSpans = sourceSpans
        if (srcSpans.isEmpty()) return null
        val first = srcSpans.first()
        val last = srcSpans.last()
        val start = lineOffsets.getOrNull(first.lineIndex)?.plus(first.columnIndex) ?: return null
        val end = lineOffsets.getOrNull(last.lineIndex)?.plus(last.columnIndex + last.length)
            ?: return null
        if (start >= end) return null
        return start to end
    }
}