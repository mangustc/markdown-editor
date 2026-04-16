package com.example.markdown_editor.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

class MarkdownVisualTransformation(
    private val annotated: AnnotatedString,
    private val spans: List<SpanInfo>,
    private val selection: TextRange,
    private val imageAspectRatios: Map<String, Float>,
    private val editorWidth: Int,
    private val density: Density
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(annotated)
        val imageSpans = spans.filter { it.type == TokenType.IMAGE }

        for (span in imageSpans) {
            val isSelected = selection.start <= span.end && selection.end >= span.start
            if (isSelected) continue

            val rawText = annotated.substring(span.start, span.end)
            val path = extractImagePath(rawText)
            val ratio = imageAspectRatios[path] ?: 1.777f
            val heightPx = if (editorWidth > 0) editorWidth / ratio else 400f
            val heightSp = with(density) { heightPx.toSp() }

            builder.addStyle(
                style = ParagraphStyle(
                    lineHeight = heightSp,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Proportional,
                        trim = LineHeightStyle.Trim.None,
                    )
                ),
                start = span.start,
                end = span.end
            )

            builder.addStyle(
                style = SpanStyle(color = Color.Transparent),
                start = span.start,
                end = span.end
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}