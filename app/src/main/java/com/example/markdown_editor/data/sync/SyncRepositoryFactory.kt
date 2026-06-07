package com.example.markdown_editor.data.sync

import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.repositories.SyncRepository
import com.example.markdown_editor.domain.usecases.sync.ValidSyncProvider
import io.ktor.client.HttpClient

class SyncRepositoryFactory(
    private val client: HttpClient,
) {
    fun create(settings: Settings): SyncRepository? {
        return when (settings.syncProvider) {
            ValidSyncProvider.YANDEX -> YandexSyncRepository(
                oauthToken = settings.yandexOauthToken,
                client = client,
            )

            ValidSyncProvider.NONE -> null
        }
    }
}