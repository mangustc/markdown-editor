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
        TokenType.IMAGE to Regex("!\\[.*?]\\((.+?)\\)"),
    )

    fun parse(text: String): List<SpanInfo> =
        rules.flatMap { (type, regex) ->
            regex.findAll(text).map { SpanInfo(it.range.first, it.range.last + 1, type) }
        }
}