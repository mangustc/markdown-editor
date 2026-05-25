package com.example.markdown_editor.domain.viewmodel.events

sealed interface ClipboardEvent : AppEvent {
    data class Save(val text: String) : ClipboardEvent
}