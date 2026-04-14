package com.example.markdown_editor.domain.parser

import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

object MarkdownParser {
    private val rules = listOf(
        TokenType.H1 to Regex("^# .+", RegexOption.MULTILINE),
        TokenType.H2 to Regex("^## .+", RegexOption.MULTILINE),
        TokenType.H3 to Regex("^### .+", RegexOption.MULTILINE),
        TokenType.BOLD to Regex("\\*\\*.+?\\*\\*"),
        TokenType.ITALIC to Regex("(?<!\\*)\\*(?!\\*).+?(?<!\\*)\\*(?!\\*)"),
        TokenType.CODE_INLINE to Regex("`[^`]+`"),
        TokenType.CODE_BLOCK to Regex("```[\\s\\S]+?```"),
    )

    fun parse(text: String): List<SpanInfo> {
        // 1. Process standard regex rules
        val standardSpans = rules.flatMap { (type, regex) ->
            regex.findAll(text).map { SpanInfo(it.range.first, it.range.last + 1, type) }
        }
        val imageSpans = parseNestedTags(text, isImage = true)
        val fileSpans = parseNestedTags(text, isImage = false)
        return standardSpans + imageSpans + fileSpans
    }

    private fun parseNestedTags(text: String, isImage: Boolean): List<SpanInfo> {
        val spans = mutableListOf<SpanInfo>()
        val startRegex = if (isImage) {
            Regex("""!\[.*?]\(""")
        } else {
            Regex("""(?<!!)\[.*?]\(""")
        }
        val tokenType = if (isImage) TokenType.IMAGE else TokenType.FILE
        startRegex.findAll(text).forEach { match ->
            val contentStart = match.range.last + 1
            var depth = 1
            for (i in contentStart until text.length) {
                when (text[i]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                if (depth == 0) {
                    spans.add(SpanInfo(match.range.first, i + 1, tokenType))
                    break
                }
            }
        }

        return spans
    }
}