package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.domain.viewmodel.events.AppEvent

interface AppGlobalActions {
    suspend fun updateNoteLists()

    fun onEvent(event: AppEvent)
}
