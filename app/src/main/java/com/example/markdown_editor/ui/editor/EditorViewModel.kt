package com.example.markdown_editor.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.domain.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel : ViewModel() {

    private val initialContent =
        "# Example Heading\n*Italic text*\n`Inline code`\n\n```\nCode block\n```"

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        onContentChanged(TextFieldValue(initialContent))
    }

    fun onContentChanged(newValue: TextFieldValue) {
        val textChanged = newValue.text != _uiState.value.textFieldValue.text

        if (!textChanged) {
            _uiState.update { it.copy(textFieldValue = newValue) }
            return
        }

        // Compute spans synchronously so textFieldValue and annotatedString
        // are always updated in the same state emission — no gap, no flash.
        val spans = MarkdownParser.parse(newValue.text)
        val annotated = MarkdownAnnotator.annotate(newValue.text, spans)

        _uiState.update {
            it.copy(
                textFieldValue = newValue,
                annotatedString = annotated
            )
        }
    }
}