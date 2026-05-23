package com.example.markdown_editor.ui.editor

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

class MarkdownOutputTransformation(
    private val state: TextFieldState,
    private val density: Density,
    private val widthProvider: () -> Int,
    private val spansProvider: () -> List<SpanInfo>,
    private val ratiosProvider: () -> Map<String, Float>,
    private val linkColor: Color,
    private val dimmedTextColor: Color,
    private val isViewingModeProvider: () -> Boolean = { false },
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val textLength = this.length
        val currentSelection = state.selection
        val currentWidth = widthProvider()
        val spans = spansProvider()
        val ratios = ratiosProvider()
        val isViewing = isViewingModeProvider()

        for (span in spans) {
            val start = span.start.coerceIn(0, textLength)
            val end = span.end.coerceIn(0, textLength)
            if (start >= end) continue

            when (span) {
                is SpanInfo.Heading -> {
                    val size = when (span.level) {
                        1 -> 2.em
                        2 -> 1.5.em
                        else -> 1.2.em
                    }
                    addStyle(
                        SpanStyle(
                            fontSize = size,
                            fontWeight = FontWeight.Bold,
                        ),
                        start, end,
                    )
                }

                is SpanInfo.Bold -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    start, end,
                )

                is SpanInfo.Italic -> addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    start, end,
                )

                is SpanInfo.CodeInline, is SpanInfo.CodeBlock -> addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                is SpanInfo.Link, is SpanInfo.File -> {
                    val rawText = this.originalText.subSequence(start, end).toString()
                    val rightBracketIndex = rawText.indexOf(']')
                    val nameStyle = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isViewingModeProvider()) {
                        if (rightBracketIndex != -1) {
                            addStyle(nameStyle, start + 1, start + rightBracketIndex)
                        } else {
                            addStyle(nameStyle, start, end)
                        }
                    } else {
                        if (rightBracketIndex != -1) {
                            val dimStyle = SpanStyle(
                                color = dimmedTextColor,
                            )
                            addStyle(dimStyle, start, start + 1)
                            addStyle(nameStyle, start + 1, start + rightBracketIndex)
                            addStyle(dimStyle, start + rightBracketIndex, end)
                        } else {
                            addStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                                start, end,
                            )
                        }
                    }
                }

                is SpanInfo.Blockquote -> addStyle(
                    SpanStyle(
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        background = Color.LightGray.copy(alpha = 0.2f),
                    ),
                    start, end,
                )

                is SpanInfo.ListItem -> {
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

                is SpanInfo.Image -> {
                    val isSelected = currentSelection.start <= end && currentSelection.end >= start
                    if (isSelected && !isViewing) {
                        addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                            ),
                            start, end,
                        )
                        continue
                    }

                    val ratio = ratios[span.payload] ?: 1.777f
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

        if (isViewing) {
            for (span in spans) {
                hideViewModeSyntax(span, textLength)
            }
        }
    }

    private fun TextFieldBuffer.hideViewModeSyntax(span: SpanInfo, textLength: Int) {
        val start = span.start.coerceIn(0, textLength)
        val end = span.end.coerceIn(0, textLength)
        if (start >= end) return

        fun hide(s: Int, e: Int) {
            val cs = s.coerceIn(0, textLength)
            val ce = e.coerceIn(0, textLength)
            if (cs < ce) addStyle(HIDDEN_STYLE, cs, ce)
        }

        when (span) {
            is SpanInfo.Heading -> {
                var markerLen = 0
                while (start + markerLen < end && originalText[start + markerLen] == '#') markerLen++
                if (start + markerLen < end && originalText[start + markerLen] == ' ') markerLen++
                hide(start, start + markerLen)
            }

            is SpanInfo.Bold -> {
                if (end - start >= 4) {
                    hide(start, start + 2)
                    hide(end - 2, end)
                }
            }

            is SpanInfo.Italic -> {
                if (end - start >= 2) {
                    hide(start, start + 1)
                    hide(end - 1, end)
                }
            }

            is SpanInfo.CodeInline -> {
                var markerLen = 0
                while (start + markerLen < end && originalText[start + markerLen] == '`') markerLen++
                if (markerLen > 0 && end - start >= markerLen * 2) {
                    hide(start, start + markerLen)
                    hide(end - markerLen, end)
                }
            }

            is SpanInfo.CodeBlock -> {
                var firstNl = start
                while (firstNl < end && originalText[firstNl] != '\n') firstNl++
                if (firstNl < end) hide(start, firstNl + 1)

                var lastNl = end - 1
                while (lastNl > start && originalText[lastNl] != '\n') lastNl--
                if (lastNl > start) hide(lastNl, end)
            }

            is SpanInfo.Link, is SpanInfo.File -> {
                val rawText = originalText.subSequence(start, end).toString()
                val bracketIdx = rawText.indexOf(']')
                if (bracketIdx != -1) {
                    hide(start, start + 1)
                    hide(start + bracketIdx, end)
                }
            }

            is SpanInfo.Blockquote -> {
                var pos = start
                while (pos < end) {
                    if (pos + 1 < end && originalText[pos] == '>' && originalText[pos + 1] == ' ') {
                        hide(pos, pos + 2)
                    } else if (originalText[pos] == '>') {
                        hide(pos, pos + 1)
                    }
                    while (pos < end && originalText[pos] != '\n') pos++
                    if (pos < end) pos++
                }
            }

            else -> {}
        }
    }

    companion object {
        private val HIDDEN_STYLE = SpanStyle(
            color = Color.Transparent,
            fontSize = 0.1.sp,
        )
    }
}