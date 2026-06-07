package com.example.markdown_editor.domain.markdown

import com.example.markdown_editor.domain.models.SpanInfo
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser as JBParser

object MarkdownParser {
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

        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = JBParser(flavour).buildMarkdownTreeFromString(text)

        val spans = mutableListOf<SpanInfo>()
        visitNode(parsedTree, text, spans)

        return spans.filter { it.range.start >= frontMatterEnd }
    }

    private fun visitNode(
        node: ASTNode,
        text: String,
        spans: MutableList<SpanInfo>,
        suppressInlineLink: Boolean = false,
    ) {
        val range = SpanInfo.TextRange(node.startOffset, node.endOffset)

        when (node.type) {
            MarkdownElementTypes.ATX_1 -> spans.add(SpanInfo.Heading(range, 1))
            MarkdownElementTypes.ATX_2 -> spans.add(SpanInfo.Heading(range, 2))
            MarkdownElementTypes.ATX_3 -> spans.add(SpanInfo.Heading(range, 3))
            MarkdownElementTypes.ATX_4 -> spans.add(SpanInfo.Heading(range, 4))
            MarkdownElementTypes.ATX_5 -> spans.add(SpanInfo.Heading(range, 5))
            MarkdownElementTypes.ATX_6 -> spans.add(SpanInfo.Heading(range, 6))
            MarkdownElementTypes.STRONG -> spans.add(SpanInfo.Bold(range))
            MarkdownElementTypes.EMPH -> spans.add(SpanInfo.Italic(range))
            MarkdownElementTypes.CODE_SPAN -> spans.add(SpanInfo.CodeInline(range))
            MarkdownElementTypes.CODE_BLOCK, MarkdownElementTypes.CODE_FENCE -> spans.add(
                SpanInfo.CodeBlock(
                    range,
                ),
            )

            MarkdownElementTypes.BLOCK_QUOTE -> spans.add(SpanInfo.Blockquote(range))
            MarkdownElementTypes.LIST_ITEM -> spans.add(SpanInfo.ListItem(range))

            MarkdownElementTypes.IMAGE -> {
                val inlineLinkNode =
                    node.children.find { it.type == MarkdownElementTypes.INLINE_LINK }
                val destParent = inlineLinkNode ?: node
                val linkDestNode =
                    destParent.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }

                val payload = if (linkDestNode != null) {
                    var s = linkDestNode.startOffset
                    var e = linkDestNode.endOffset
                    if (s < e && text[s] == '<' && text[e - 1] == '>') {
                        s++
                        e--
                    }
                    if (s < e) text.substring(s, e) else ""
                } else ""

                spans.add(SpanInfo.Image(range, payload))
            }

            MarkdownElementTypes.INLINE_LINK -> {
                if (!suppressInlineLink) {
                    val linkTextNode =
                        node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                    val linkDestNode =
                        node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }

                    val labelRange = if (linkTextNode != null) {
                        var s = linkTextNode.startOffset
                        var e = linkTextNode.endOffset
                        if (s < e && text[s] == '[' && text[e - 1] == ']') {
                            s++
                            e--
                        }
                        SpanInfo.TextRange(s, e)
                    } else range

                    val payloadRange = if (linkDestNode != null) {
                        var s = linkDestNode.startOffset
                        var e = linkDestNode.endOffset
                        if (s < e && text[s] == '<' && text[e - 1] == '>') {
                            s++
                            e--
                        }
                        SpanInfo.TextRange(s, e)
                    } else range

                    val label = if (labelRange.start < labelRange.end) {
                        text.substring(labelRange.start, labelRange.end)
                    } else ""

                    val payload = if (payloadRange.start < payloadRange.end) {
                        text.substring(payloadRange.start, payloadRange.end)
                    } else ""

                    spans.add(SpanInfo.Link(range, payload, label, payloadRange, labelRange))
                }
            }
        }

        val passSuppress = node.type == MarkdownElementTypes.IMAGE
        for (child in node.children) {
            visitNode(child, text, spans, passSuppress)
        }
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
}