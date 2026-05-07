package com.example.markdown_editor.domain.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

object MarkdownAnnotator {
    fun annotate(text: String, spans: List<SpanInfo>): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            spans.forEach { info ->
                if (info.start >= info.end || info.end > text.length) return@forEach
                when (info.type) {
                    TokenType.H1 -> addStyle(
                        SpanStyle(
                            fontSize = 2.em,
                            fontWeight = FontWeight.Bold,
                        ),
                        info.start, info.end,
                    )

                    TokenType.H2 -> addStyle(
                        SpanStyle(
                            fontSize = 1.5.em,
                            fontWeight = FontWeight.Bold,
                        ),
                        info.start, info.end,
                    )

                    TokenType.H3 -> addStyle(
                        SpanStyle(
                            fontSize = 1.2.em,
                            fontWeight = FontWeight.Bold,
                        ),
                        info.start, info.end,
                    )

                    TokenType.BOLD -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        info.start,
                        info.end,
                    )

                    TokenType.ITALIC -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        info.start,
                        info.end,
                    )

                    TokenType.CODE_INLINE, TokenType.CODE_BLOCK -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.2f),
                        ),
                        info.start, info.end,
                    )

                    TokenType.IMAGE -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                        info.start, info.end,
                    )

                    TokenType.FILE -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        info.start,
                        info.end,
                    )

                    TokenType.LINK -> addStyle(
                        SpanStyle(
                            color = Color(0xFF0055CC),
                            textDecoration = TextDecoration.Underline,
                        ),
                        info.start, info.end,
                    )

                    TokenType.BLOCKQUOTE -> addStyle(
                        SpanStyle(
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic,
                            background = Color.LightGray.copy(alpha = 0.2f),
                        ),
                        info.start, info.end,
                    )

                    TokenType.LIST_ITEM -> {} // Layout handled in VisualTransformation
                }
            }
        }
    }
}