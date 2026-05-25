package com.example.markdown_editor.domain.viewmodel.events

sealed interface FocusEvent : AppEvent {
    data object ClearFocus : FocusEvent
}