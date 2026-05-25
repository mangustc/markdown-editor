package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.data.model.Note

sealed interface NavigationEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
}

sealed interface NotificationEvent {
    data object FailedToAddPhoto : NotificationEvent
    data object FailedToStartCamera : NotificationEvent
    data object NoAppFoundToOpenThisFile : NotificationEvent
    data object LinkCopied : NotificationEvent
}
