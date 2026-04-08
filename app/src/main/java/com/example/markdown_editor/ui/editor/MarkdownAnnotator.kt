package com.example.markdown_editor.ui.editor

import androidx.compose.ui.graphics.Color
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
                            color = Color(0xFF1A73E8),
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
                            background = Color(0xFFF5F5F5),
                            color = Color(0xFFC7254E)
                        ), info.start, info.end
                    )
                    TokenType.CODE_BLOCK -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFFF8F8F8)
                        ), info.start, info.end
                    )
                    else -> Unit
                }
            }
        }
    }
}