package com.example.markdown_editor.ui.editor

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import com.example.markdown_editor.ui.viewmodel.EditorEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Load note content whenever the active URI changes
    LaunchedEffect(uiState.activeNote) {
        viewModel.editorOnNoteOpened()
    }
    val visualTransformation = remember(uiState.editorAnnotatedString) {
        MarkdownVisualTransformation(uiState.editorAnnotatedString)
    }

    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Scaffold(
        // No TopAppBar here — AppScaffold owns that
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(), // always sits above the keyboard
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "****",
                            cursorOffset = 2
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "**",
                            cursorOffset = 1
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "``",
                            cursorOffset = 1
                        )
                    )
                }) {
                    Icon(Icons.Default.Code, contentDescription = "Inline code")
                }
                IconButton(onClick = { viewModel.editorOnSave() }) {
                    Icon(Icons.Default.Save, contentDescription = "Save file")
                }
            }
        }
    ) { innerPadding ->
        BasicTextField(
            value = uiState.editorTextFieldValue,
            onValueChange = { viewModel.editorOnContentChanged(it) },
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // imeNestedScroll lets the scroll state react to keyboard appearance
                .imeNestedScroll()
                .verticalScroll(scrollState)
                .bringIntoViewRequester(bringIntoViewRequester)
                .padding(16.dp)
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        scope.launch {
                            // Small delay lets the keyboard finish animating in
                            // before we scroll — without this it scrolls to the
                            // wrong position
                            kotlinx.coroutines.delay(300)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                }
        )
    }
}