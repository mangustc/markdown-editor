package com.example.markdown_editor.domain.viewmodel

import com.example.markdown_editor.data.model.Settings
import com.example.markdown_editor.data.sync.ValidSyncProvider
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

    fun setSyncProvider(provider: ValidSyncProvider) {
        val settings = getSettings() ?: return
        val newSettings = settings.copy(
            syncProvider = provider,
        )
        updateSettings(newSettings)
    }

    fun setYandexOauthToken(token: String) {
        val settings = getSettings() ?: return
        val newSettings = settings.copy(
            yandexOauthToken = token,
        )
        updateSettings(newSettings)
    }

    private fun getSettings(): Settings? {
        val project = deps.uiState.value.project ?: return null
        val settings = deps.settingsRepo.getSettings(project)
        return settings
    }

    private fun updateSettings(newSettings: Settings) {
        val project = deps.uiState.value.project ?: return
        val settings = deps.settingsRepo.setSettings(project, newSettings)
        deps.uiState.update { it.copy(settings = settings) }
    }
}