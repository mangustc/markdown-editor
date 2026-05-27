package com.example.markdown_editor.ui.viewmodel.events

sealed interface ClipboardEvent : AppEvent {
    data class Copy(val text: String) : ClipboardEvent
}