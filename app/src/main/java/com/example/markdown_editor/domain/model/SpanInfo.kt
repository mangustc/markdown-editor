package com.example.markdown_editor.domain.model

data class SpanInfo(val start: Int, val end: Int, val type: TokenType)

enum class TokenType { H1, H2, H3, BOLD, ITALIC, CODE_INLINE, CODE_BLOCK, IMAGE, FILE }