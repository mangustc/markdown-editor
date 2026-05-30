package com.example.markdown_editor.ui.viewmodel.events

sealed interface NotificationEvent : AppEvent {
    data class CustomMessage(val message: String) : NotificationEvent
    data class FromException(val exception: Exception) : NotificationEvent

    data object FailedToAddPhoto : NotificationEvent
    data object FailedToStartCamera : NotificationEvent
    data object NoAppFoundToOpenThisFile : NotificationEvent
    data object LinkCopied : NotificationEvent
    data object SyncServiceIsNone : NotificationEvent
}