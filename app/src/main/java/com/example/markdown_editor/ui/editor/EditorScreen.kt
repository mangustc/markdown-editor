package com.example.markdown_editor.ui.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.collectAsState
import android.widget.TextView
import com.example.markdown_editor.widget.MarkdownEditText
import com.example.markdown_editor.widget.SpanRenderer

@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    // State flows DOWN from ViewModel into this composable
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            factory = { context ->
                MarkdownEditText(context).apply {
                    setText(
                        "# Example Heading\n*Italic text*\n`Inline code`\n\n```\nCode block\n```",
                        TextView.BufferType.SPANNABLE
                    )
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()

                    // Event flows UP — widget tells ViewModel text changed
                    onContentChanged = { text ->
                        viewModel.onContentChanged(text)
                    }
                }
            },
            update = { editText ->
                // State flows DOWN — ViewModel tells the widget what spans to render
                // Guard: only apply spans when text matches to avoid stale span positions
                if (editText.text?.toString() == uiState.content) {
                    editText.text?.let { editable ->
                        SpanRenderer.apply(editable, uiState.spans)
                    }
                }
            }
        )
    }
}