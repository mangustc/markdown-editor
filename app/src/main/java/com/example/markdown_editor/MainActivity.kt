package com.example.markdown_editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.markdown_editor.ui.AppScaffold
import com.example.markdown_editor.ui.theme.MarkdowneditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkdowneditorTheme(darkTheme = false) {
                AppScaffold()
            }
        }
    }
}