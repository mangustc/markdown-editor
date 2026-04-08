package com.example.markdown_editor.ui.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EditorScreen(viewModel: EditorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val visualTransformation = remember(uiState.annotatedString) {
        MarkdownVisualTransformation(uiState.annotatedString)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BasicTextField(
            value = uiState.textFieldValue,
            onValueChange = { viewModel.onContentChanged(it) },
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        )
    }
}