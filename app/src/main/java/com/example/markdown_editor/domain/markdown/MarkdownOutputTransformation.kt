package com.example.markdown_editor.domain.markdown

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

class MarkdownOutputTransformation(
    private val state: TextFieldState,
    private val density: Density,
    private val widthProvider: () -> Int,
    private val spansProvider: () -> List<SpanInfo>,
    private val ratiosProvider: () -> Map<String, Float>,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val textLength = this.length
        val currentSelection = state.selection
        val currentWidth = widthProvider()
        val spans = spansProvider()
        val ratios = ratiosProvider()

        for (span in spans) {
            val start = span.start.coerceIn(0, textLength)
            val end = span.end.coerceIn(0, textLength)
            if (start >= end) continue

            when (span.type) {
                TokenType.H1 -> addStyle(
                    SpanStyle(
                        fontSize = 2.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.H2 -> addStyle(
                    SpanStyle(
                        fontSize = 1.5.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.H3 -> addStyle(
                    SpanStyle(
                        fontSize = 1.2.em,
                        fontWeight = FontWeight.Bold,
                    ),
                    start, end,
                )

                TokenType.BOLD -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end,
                )

                TokenType.ITALIC -> addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    start,
                    end,
                )

                TokenType.CODE_INLINE, TokenType.CODE_BLOCK -> addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                TokenType.LINK -> addStyle(
                    SpanStyle(color = Color(0xFF0055CC), textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )

                TokenType.BLOCKQUOTE -> addStyle(
                    SpanStyle(
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        background = Color.LightGray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                TokenType.FILE -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start,
                    end,
                )

                TokenType.LIST_ITEM -> {
                    var actualStart = start
                    while (actualStart > 0 && this.originalText[actualStart - 1] != '\n') {
                        actualStart--
                    }

                    var actualEnd = end
                    while (actualEnd < textLength && this.originalText[actualEnd] != '\n') {
                        actualEnd++
                    }
                    if (actualEnd < textLength) actualEnd++

                    addStyle(
                        ParagraphStyle(
                            textIndent = TextIndent(
                                firstLine = 0.sp,
                                restLine = 8.sp,
                            ),
                        ),
                        actualStart, actualEnd,
                    )
                    addStyle(SpanStyle(), start, end)
                }

                TokenType.IMAGE -> {
                    val isSelected = currentSelection.start <= end && currentSelection.end >= start
                    if (isSelected) {
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                            ),
                            start, end,
                        )
                        continue
                    }

                    val path = span.payload ?: continue
                    val ratio = ratios[path] ?: 1.777f
                    val heightPx = if (currentWidth > 0) currentWidth / ratio else 400f
                    val heightSp = with(density) { heightPx.toSp() }

                    addStyle(
                        ParagraphStyle(
                            lineHeight = heightSp,
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Proportional,
                                trim = LineHeightStyle.Trim.None,
                            ),
                        ),
                        start, end,
                    )
                    addStyle(SpanStyle(color = Color.Transparent), start, end)
                }
            }
        }
    }
}
