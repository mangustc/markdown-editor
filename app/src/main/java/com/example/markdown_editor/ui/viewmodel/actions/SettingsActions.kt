package com.example.markdown_editor.ui.viewmodel.actions

import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.usecases.settings.SetSettingsInput
import com.example.markdown_editor.domain.usecases.settings.SetSettingsUseCase
import com.example.markdown_editor.domain.usecases.sync.ValidSyncProvider
import com.example.markdown_editor.ui.util.runUseCase
import com.example.markdown_editor.ui.viewmodel.AppDeps
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val setSettingsUseCase: SetSettingsUseCase by inject()

    fun showSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun dismissSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

    fun setSyncProvider(provider: ValidSyncProvider) {
        val settings = deps.uiState.value.settings ?: return
        val newSettings = settings.copy(
            syncProvider = provider,
        )
        updateSettings(newSettings)
    }

    fun setYandexOauthToken(token: String) {
        val settings = deps.uiState.value.settings ?: return
        val newSettings = settings.copy(
            yandexOauthToken = token,
        )
        updateSettings(newSettings)
    }

    private fun updateSettings(newSettings: Settings) = deps.scope.launch {
        val project = deps.uiState.value.project ?: return@launch
        deps.uiState.update { it.copy(settings = newSettings) }
        runUseCase(deps.globalActions::onEvent) {
            setSettingsUseCase(
                SetSettingsInput(
                    project = project,
                    newSettings = newSettings,
                ),
            )
        }.getOrElse { return@launch }
    }
}