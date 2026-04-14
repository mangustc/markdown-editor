package com.example.markdown_editor.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType

class MarkdownVisualTransformation(
    private val annotated: AnnotatedString,
    private val spans: List<SpanInfo>,
) : VisualTransformation {
    companion object {
        const val SPACER = "\n\n\n\n\n\n\n\n\n\n\n"
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder()
        val imageSpans = spans.filter { it.type == TokenType.IMAGE }.sortedBy { it.start }

        var lastEnd = 0
        for (span in imageSpans) {
            builder.append(annotated.subSequence(lastEnd, span.end))
            builder.append(SPACER)
            lastEnd = span.end
        }
        builder.append(annotated.subSequence(lastEnd, annotated.length))

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var shift = 0
                for (span in imageSpans) {
                    if (offset >= span.end) shift += SPACER.length
                    else break
                }
                return offset + shift
            }

            override fun transformedToOriginal(offset: Int): Int {
                var shift = 0
                for (span in imageSpans) {
                    val spacerStart = span.end + shift
                    val spacerEnd = spacerStart + SPACER.length
                    if (offset >= spacerEnd) {
                        shift += SPACER.length
                    } else if (offset > spacerStart) {
                        return span.end
                    } else break
                }
                return offset - shift
            }
        }
        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}