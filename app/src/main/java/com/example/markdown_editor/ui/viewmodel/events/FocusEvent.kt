package com.example.markdown_editor.ui.viewmodel.events

sealed interface FocusEvent : AppEvent {
    data object ClearFocus : FocusEvent
}