package com.example.markdown_editor.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
                            fontWeight = FontWeight.Bold
                        ), info.start, info.end
                    )

                    TokenType.H2 -> addStyle(
                        SpanStyle(
                            fontSize = 1.5.em,
                            fontWeight = FontWeight.Bold
                        ), info.start, info.end
                    )

                    TokenType.H3 -> addStyle(
                        SpanStyle(
                            fontSize = 1.2.em,
                            fontWeight = FontWeight.Bold
                        ), info.start, info.end
                    )

                    TokenType.BOLD -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        info.start, info.end
                    )

                    TokenType.ITALIC -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        info.start, info.end
                    )

                    TokenType.CODE_INLINE -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                        ), info.start, info.end
                    )

                    TokenType.CODE_BLOCK -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                        ), info.start, info.end
                    )

                    TokenType.IMAGE -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                        ),
                        info.start, info.end
                    )

                    TokenType.FILE -> addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                        ),
                        info.start, info.end
                    )
                }
            }
        }
    }
}