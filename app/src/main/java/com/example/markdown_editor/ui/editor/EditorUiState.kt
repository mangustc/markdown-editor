package com.example.markdown_editor.ui.editor

import com.example.markdown_editor.domain.model.SpanInfo

data class EditorUiState(
    val content: String = "",
    val spans: List<SpanInfo> = emptyList()
)