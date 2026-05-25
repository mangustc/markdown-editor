package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.data.model.Note

sealed interface NavigationEvent {
    data class GoToEditor(val note: Note) : NavigationEvent
    data object GoBack : NavigationEvent
    data object OpenDrawer : NavigationEvent
    data object CloseDrawer : NavigationEvent
}

sealed interface NotificationEvent {
    data class CustomMessage(val message: String) : NotificationEvent
    data object FailedToAddPhoto : NotificationEvent
    data object FailedToStartCamera : NotificationEvent
    data object NoAppFoundToOpenThisFile : NotificationEvent
    data object LinkCopied : NotificationEvent

    data object SyncAuthException : NotificationEvent
    data object SyncNetworkException : NotificationEvent
    data object SyncServerException : NotificationEvent
    data object SyncLocalIoException : NotificationEvent
    data object SyncStateException : NotificationEvent
    data object SyncQuotaException : NotificationEvent
}
