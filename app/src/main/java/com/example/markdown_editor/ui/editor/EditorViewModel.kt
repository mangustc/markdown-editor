package com.example.markdown_editor.ui.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.domain.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProjectRepositoryImpl(
        application,
    )
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.InsertSyntax -> insertSyntax(event.syntax, event.cursorOffset)
        }
    }

    private fun insertSyntax(syntax: String, cursorOffset: Int) {
        val current = _uiState.value.textFieldValue
        val cursor = current.selection.start
        val newText = current.text.substring(0, cursor) + syntax + current.text.substring(cursor)
        val newCursor = cursor + cursorOffset
        val newValue = current.copy(
            text = newText,  // TextFieldValue accepts plain text here
            selection = androidx.compose.ui.text.TextRange(newCursor)
        )
        onContentChanged(newValue)
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

    fun onNoteOpened(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = repository.getNoteByUri(uri)
            val text = repository.getNoteText(note)
            withContext(Dispatchers.Main) {
                onContentChanged(TextFieldValue(text))
            }
            // Track URI so save knows where to write
            _uiState.update { it.copy(note = note) }
        }
    }

    fun onSave() {
        val note = _uiState.value.note ?: return
        val text = _uiState.value.textFieldValue.text
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNoteText(note, text)
        }
    }
}