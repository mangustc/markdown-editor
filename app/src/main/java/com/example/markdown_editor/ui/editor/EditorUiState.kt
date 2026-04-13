package com.example.markdown_editor.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import com.example.markdown_editor.data.model.Note

data class EditorUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val annotatedString: AnnotatedString = AnnotatedString(""),
    val note: Note? = null
)