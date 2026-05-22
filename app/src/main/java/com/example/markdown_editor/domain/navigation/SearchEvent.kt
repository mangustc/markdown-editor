package com.example.markdown_editor.domain.navigation

sealed class SearchEvent {
    object AppendTag : SearchEvent()
    object AppendName : SearchEvent()
    object ToggleNegation : SearchEvent()
    object Clear : SearchEvent()
}