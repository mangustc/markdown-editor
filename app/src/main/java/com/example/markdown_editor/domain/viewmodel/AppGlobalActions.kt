package com.example.markdown_editor.domain.viewmodel

interface AppGlobalActions {
    fun updateNoteLists(
        afterUpdateSearch: () -> Unit = {},
        afterUpdateMessenger: () -> Unit = {},
    )

    fun navigationEvent(navigationEvent: NavigationEvent)
    fun showToast(notificationEvent: NotificationEvent)
}
