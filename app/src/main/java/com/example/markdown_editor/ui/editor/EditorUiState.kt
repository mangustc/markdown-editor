package com.example.markdown_editor.ui.editor

import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue

data class EditorUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(),
    val annotatedString: AnnotatedString = AnnotatedString(""),
    val activeNoteUri: Uri? = null
)