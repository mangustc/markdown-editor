package com.example.markdown_editor.domain.models

import com.example.markdown_editor.domain.usecases.sync.ValidSyncProvider
import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val reverseLayout: Boolean,
    val syncProvider: ValidSyncProvider,
    val yandexOauthToken: String,
) {
    companion object {
        val EMPTY = Settings(
            reverseLayout = false,
            syncProvider = ValidSyncProvider.NONE,
            yandexOauthToken = "",
        )
    }
}