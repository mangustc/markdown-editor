package com.example.markdown_editor.domain.parser

import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

object MarkdownParser {
    // Exposed so other layers (e.g. messenger body parser) can reuse the same
    // battle-tested regexes instead of duplicating them with subtle differences.
    //
    // Both patterns use the CommonMark alternation:
    //   1. <[^>]*>  – angle-bracket destination: any character allowed, including ')'
    //   2. .+?      – bare destination: lazy, so ')' terminates as per spec
    val IMAGE_REGEX = Regex("""!\[.*?]\((?:<.+?>|.+?)\)""")
    val FILE_REGEX = Regex("""(?<!!)\[.*?]\((?:<.+?>|.+?)\)""")

    private val rules = listOf(
        TokenType.H1 to Regex("""^# .+""", RegexOption.MULTILINE),
        TokenType.H2 to Regex("""^## .+""", RegexOption.MULTILINE),
        TokenType.H3 to Regex("""^### .+""", RegexOption.MULTILINE),
        TokenType.BOLD to Regex("""\*\*.+?\*\*"""),
        TokenType.ITALIC to Regex("""(?<!\*)\*(?!\*).+?(?<!\*)\*(?!\*)"""),
        TokenType.CODE_INLINE to Regex("""`[^`]+`"""),
        TokenType.CODE_BLOCK to Regex("""```[\s\S]+?```"""),
        TokenType.IMAGE to IMAGE_REGEX,
        TokenType.FILE to FILE_REGEX,
    )

    fun parse(text: String): List<SpanInfo> =
        rules.flatMap { (type, regex) ->
            regex.findAll(text).map { SpanInfo(it.range.first, it.range.last + 1, type) }
        }
}