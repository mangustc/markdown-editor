package com.example.markdown_editor.ui.navigation

sealed class AppDestination(val route: String) {
    data object Editor : AppDestination("editor")
    data object Messenger : AppDestination("messenger")
}