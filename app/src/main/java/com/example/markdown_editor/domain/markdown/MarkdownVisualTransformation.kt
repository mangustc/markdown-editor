package com.example.markdown_editor.domain.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

class MarkdownVisualTransformation(
    private val spans: List<SpanInfo>,
    private val selection: TextRange,
    private val imageAspectRatios: Map<String, Float>,
    private val editorWidth: Int,
    private val density: Density,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        val textLength = text.length

        for (span in spans) {
            val start = span.start.coerceIn(0, textLength)
            val end = span.end.coerceIn(0, textLength)
            if (start >= end) continue

            when (span.type) {
                TokenType.H1 -> builder.addStyle(
                    SpanStyle(
                        fontSize = 2.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.H2 -> builder.addStyle(
                    SpanStyle(
                        fontSize = 1.5.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.H3 -> builder.addStyle(
                    SpanStyle(
                        fontSize = 1.2.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.BOLD -> builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end,
                )

                TokenType.ITALIC -> builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    start,
                    end,
                )

                TokenType.CODE_INLINE, TokenType.CODE_BLOCK -> builder.addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                TokenType.LINK -> builder.addStyle(
                    SpanStyle(color = Color(0xFF0055CC), textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )

                TokenType.BLOCKQUOTE -> builder.addStyle(
                    SpanStyle(
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        background = Color.LightGray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                TokenType.FILE -> builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end,
                )

                TokenType.LIST_ITEM -> {
//                    builder.addStyle(
//                        ParagraphStyle(
//                            textIndent = TextIndent(
//                                firstLine = 0.sp,
//                                restLine = 24.sp,
//                            ),
//                        ),
//                        start, end,
//                    )
                }

                TokenType.IMAGE -> {
                    val isSelected = selection.start <= end && selection.end >= start
                    if (isSelected) {
                        builder.addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                            ),
                            start, end,
                        )
                        continue
                    }

                    val path = span.payload ?: continue
                    val ratio = imageAspectRatios[path] ?: 1.777f
                    val heightPx = if (editorWidth > 0) editorWidth / ratio else 400f
                    val heightSp = with(density) { heightPx.toSp() }

                    builder.addStyle(
                        style = ParagraphStyle(
                            lineHeight = heightSp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Proportional,
                                trim = LineHeightStyle.Trim.None,
                            ),
                        ),
                        start, end,
                    )
                    builder.addStyle(SpanStyle(color = Color.Transparent), start, end)
                }
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}