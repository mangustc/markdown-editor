package com.example.markdown_editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.markdown_editor.ui.editor.EditorScreen
import com.example.markdown_editor.ui.editor.EditorViewModel
import com.example.markdown_editor.ui.theme.MarkdowneditorTheme

class MainActivity : ComponentActivity() {
    private val editorViewModel: EditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkdowneditorTheme(darkTheme = false) {
                EditorScreen(editorViewModel)
            }
        }
    }
}