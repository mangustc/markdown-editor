package com.example.markdown_editor.domain.viewmodel.events

import com.example.markdown_editor.data.model.Note

sealed interface NavigationEvent : AppEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
}