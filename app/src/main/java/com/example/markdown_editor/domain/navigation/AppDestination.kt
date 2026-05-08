package com.example.markdown_editor.domain.navigation

import kotlinx.serialization.Serializable

@Serializable
data class EditorDestination(val noteUriString: String)

@Serializable
object MessengerDestination