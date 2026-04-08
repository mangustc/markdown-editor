package com.example.markdown_editor.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class MarkdownVisualTransformation(
    private val annotated: AnnotatedString
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        // Text length is identical — offset mapping is 1:1
        return TransformedText(
            text = if (annotated.text == text.text) annotated else text,
            offsetMapping = OffsetMapping.Identity
        )
    }
}