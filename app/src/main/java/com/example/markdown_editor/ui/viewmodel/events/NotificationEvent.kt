package com.example.markdown_editor.ui.viewmodel.events

sealed interface NotificationEvent : AppEvent {
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
    data object SyncServiceIsNone : NotificationEvent
}