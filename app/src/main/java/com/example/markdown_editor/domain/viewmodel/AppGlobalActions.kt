package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.data.model.Note

interface AppGlobalActions {
    fun updateNoteLists(
        afterUpdateSearch: () -> Unit = {},
        afterUpdateMessenger: () -> Unit = {},
    )

    fun goToEditor(note: Note)
    fun goBack()
    fun openDrawer()
    fun closeDrawer()
}
