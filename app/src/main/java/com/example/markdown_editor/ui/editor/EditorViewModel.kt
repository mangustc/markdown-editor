package com.example.markdown_editor.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.domain.parser.MarkdownParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var parseJob: Job? = null

    // Called by the UI when the user types — this is the "event flowing up"
    fun onContentChanged(text: String) {
        // Optimistically update content immediately so the cursor never fights us
        _uiState.update { it.copy(content = text) }

        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            delay(120) // debounce
            val spans = withContext(Dispatchers.Default) {
                MarkdownParser.parse(text)
            }
            _uiState.update { it.copy(spans = spans) }
        }
    }
}