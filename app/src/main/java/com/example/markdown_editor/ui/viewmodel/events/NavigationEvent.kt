package com.example.markdown_editor.ui.viewmodel.events

import android.net.Uri
import com.example.markdown_editor.data.model.Note

sealed interface NavigationEvent : AppEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
    data class OpenFile(val uri: Uri) : NavigationEvent
    data class OpenUrl(val url: String) : NavigationEvent
}