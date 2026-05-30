package com.example.markdown_editor.ui.util

import com.example.markdown_editor.ui.viewmodel.events.AppEvent
import com.example.markdown_editor.ui.viewmodel.events.NotificationEvent

inline fun <R> runUseCase(onEvent: (AppEvent) -> Unit, block: () -> R): Result<R> =
    try {
        Result.success(block())
    } catch (e: Exception) {
        e.printStackTrace()
        onEvent(NotificationEvent.FromException(e))
        Result.failure(e)
    }
