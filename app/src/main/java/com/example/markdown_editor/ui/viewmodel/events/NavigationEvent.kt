package com.example.markdown_editor.ui.viewmodel.events

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note

sealed interface NavigationEvent : AppEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
    data class OpenFile(val uri: FileSystemPath) : NavigationEvent
    data class OpenUrl(val url: String) : NavigationEvent
}