package com.example.markdown_editor.ui.viewmodel

import com.example.markdown_editor.ui.viewmodel.events.AppEvent

interface AppGlobalActions {
    suspend fun updateNoteLists()

    fun onEvent(event: AppEvent)
}
