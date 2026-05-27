package com.example.markdown_editor.data.sync

import com.example.markdown_editor.data.model.Settings
import com.example.markdown_editor.domain.usecases.sync.ValidSyncProvider

class SyncRepositoryFactory {
    fun create(settings: Settings): SyncRepository? {
        return when (settings.syncProvider) {
            ValidSyncProvider.YANDEX -> YandexSyncRepository(
                oauthToken = settings.yandexOauthToken,
            )

            ValidSyncProvider.NONE -> null
        }
    }
}