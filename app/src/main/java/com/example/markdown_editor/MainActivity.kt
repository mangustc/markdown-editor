package com.example.markdown_editor

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.markdown_editor.ui.AppScaffold
import com.example.markdown_editor.ui.theme.MarkdowneditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        setContent {
            MarkdowneditorTheme(darkTheme = false) {
                AppScaffold()
            }
        }
    }
}