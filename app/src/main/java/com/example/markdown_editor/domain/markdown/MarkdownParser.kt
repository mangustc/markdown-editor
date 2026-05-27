package com.example.markdown_editor.domain.markdown

import com.example.markdown_editor.domain.models.SpanInfo
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
                        ?.let { spans.add(SpanInfo.Heading(it, heading.level)) }
                    visitChildren(heading)
                }

                override fun visit(strongEmphasis: StrongEmphasis) {
                    strongEmphasis.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.Bold(it)) }
                    visitChildren(strongEmphasis)
                }

                override fun visit(emphasis: Emphasis) {
                    emphasis.bounds(lineOffsets)?.let { spans.add(SpanInfo.Italic(it)) }
                    visitChildren(emphasis)
                }

                override fun visit(code: Code) {
                    code.bounds(lineOffsets)?.let { spans.add(SpanInfo.CodeInline(it)) }
                }

                override fun visit(fencedCodeBlock: FencedCodeBlock) {
                    fencedCodeBlock.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.CodeBlock(it)) }
                }

                override fun visit(indentedCodeBlock: IndentedCodeBlock) {
                    indentedCodeBlock.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.CodeBlock(it)) }
                }

                override fun visit(image: Image) {
                    image.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.Image(it, image.destination)) }
                }

                override fun visit(link: Link) {
                    val label = (link.firstChild as? Text)?.literal ?: link.destination
                    link.bounds(lineOffsets)?.let {
                        val rawText = text.substring(it.start, it.end)

                        var child = link.firstChild
                        var firstChildStart: Int? = null
                        var lastChildEnd: Int? = null
                        while (child != null) {
                            val childBounds = child.bounds(lineOffsets)
                            if (childBounds != null) {
                                if (firstChildStart == null) firstChildStart = childBounds.start
                                lastChildEnd = childBounds.end
                            }
                            child = child.next
                        }

                        val rightBracketIndex = rawText.indexOf(']')
                        val labelRange = if (firstChildStart != null && lastChildEnd != null) {
                            SpanInfo.TextRange(firstChildStart, lastChildEnd)
                        } else if (rightBracketIndex != -1) {
                            SpanInfo.TextRange(it.start + 1, it.start + rightBracketIndex)
                        } else {
                            it
                        }

                        val leftParenIndex =
                            rawText.indexOf('(', rightBracketIndex.coerceAtLeast(0))
                        val rightParenIndex = rawText.lastIndexOf(')')
                        val payloadRange =
                            if (leftParenIndex != -1 && rightParenIndex > leftParenIndex) {
                                var pStart = it.start + leftParenIndex + 1
                                var pEnd = it.start + rightParenIndex

                                while (pStart < pEnd && text[pStart].isWhitespace()) {
                                    pStart++
                                }
                                while (pEnd > pStart && text[pEnd - 1].isWhitespace()) {
                                    pEnd--
                                }

                                if (pStart < pEnd && text[pStart] == '<' && text[pEnd - 1] == '>') {
                                    pStart++
                                    pEnd--
                                }

                                if (!link.title.isNullOrEmpty()) {
                                    var titleEnd = pEnd
                                    while (titleEnd > pStart && text[titleEnd - 1].isWhitespace()) {
                                        titleEnd--
                                    }
                                    if (titleEnd > pStart) {
                                        val lastChar = text[titleEnd - 1]
                                        if (lastChar == '"' || lastChar == '\'' || lastChar == ')') {
                                            val openChar = if (lastChar == ')') '(' else lastChar
                                            var titleStart = titleEnd - 2
                                            while (titleStart > pStart) {
                                                if (text[titleStart] == openChar) {
                                                    var backslashes = 0
                                                    var temp = titleStart - 1
                                                    while (temp >= pStart && text[temp] == '\\') {
                                                        backslashes++
                                                        temp--
                                                    }
                                                    if (backslashes % 2 == 0) {
                                                        break
                                                    }
                                                }
                                                titleStart--
                                            }
                                            if (titleStart > pStart) {
                                                pEnd = titleStart
                                                while (pEnd > pStart && text[pEnd - 1].isWhitespace()) {
                                                    pEnd--
                                                }
                                            }
                                        }
                                    }
                                }
                                SpanInfo.TextRange(pStart, pEnd)
                            } else {
                                SpanInfo.TextRange(it.end, it.end)
                            }

                        spans.add(
                            SpanInfo.Link(
                                range = it,
                                payload = link.destination,
                                label = label,
                                payloadRange = payloadRange,
                                labelRange = labelRange,
                            ),
                        )
                    }
                    visitChildren(link)
                }

                override fun visit(listItem: ListItem) {
                    listItem.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.ListItem(it)) }
                    visitChildren(listItem)
                }

                override fun visit(blockQuote: BlockQuote) {
                    blockQuote.bounds(lineOffsets)
                        ?.let { spans.add(SpanInfo.Blockquote(it)) }
                    visitChildren(blockQuote)
                }
            },
        )
        return spans.filter { it.range.start >= frontMatterEnd }
    }

    fun stripAttachments(text: String, spans: List<SpanInfo>): String {
        val toRemove = spans.filter { it is SpanInfo.Image || it is SpanInfo.Link }
            .sortedByDescending { it.range.start }
        var res = text
        for (span in toRemove) {
            val start = span.range.start
            val end = span.range.end
            if (start < end && end <= res.length) {
                res = res.removeRange(start, end)
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

    private fun Node.bounds(lineOffsets: IntArray): SpanInfo.TextRange? {
        val srcSpans = sourceSpans
        if (srcSpans.isEmpty()) return null
        val first = srcSpans.first()
        val last = srcSpans.last()
        val start = lineOffsets.getOrNull(first.lineIndex)?.plus(first.columnIndex) ?: return null
        val end = lineOffsets.getOrNull(last.lineIndex)?.plus(last.columnIndex + last.length)
            ?: return null
        if (start >= end) return null
        return SpanInfo.TextRange(start, end)
    }
}