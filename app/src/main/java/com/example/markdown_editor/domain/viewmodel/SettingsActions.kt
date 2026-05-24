package com.example.markdown_editor.domain.viewmodel

import kotlinx.coroutines.flow.update

class SettingsActions(
    private val deps: AppDeps,
) {
    fun showSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun dismissSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

    fun setSyncProvider(provider: String) {
        deps.uiState.update { it.copy(syncProvider = provider) }
    }

    fun setYandexOauthToken(token: String) {
        deps.uiState.update { it.copy(yandexOauthToken = token) }
    }
}