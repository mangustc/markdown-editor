package com.example.markdown_editor.domain.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.markdown_editor.data.sync.YandexDiskProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActions(
    private val deps: AppDeps,
) {
    fun loadSavedSettings() {
        deps.scope.launch {
            withContext(Dispatchers.IO) {
                val prefs: SharedPreferences = deps.context.getSharedPreferences(
                    "project_prefs",
                    Context.MODE_PRIVATE,
                )
                val provider = prefs.getString("sync_provider", null) ?: return@withContext
                val yandexOauthToken =
                    prefs.getString("yandex_oauth_token", null) ?: return@withContext
                setSyncProvider(provider)
                setYandexOauthToken(yandexOauthToken)
            }
        }
    }

    fun showSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = true) }
    }

    fun dismissSettings() {
        deps.uiState.update { it.copy(isSettingsDialogVisible = false) }
    }

    fun setSyncProvider(provider: String) {
        val realProvider = if (provider != "None" && provider != "Yandex Disk") "None" else provider
        val prefs: SharedPreferences = deps.context.getSharedPreferences(
            "project_prefs",
            Context.MODE_PRIVATE,
        )
        prefs.edit {
            putString("sync_provider", realProvider)
        }
        deps.uiState.update { it.copy(syncProvider = realProvider) }
        applyProviderConfig(realProvider, deps.uiState.value.yandexOauthToken)
    }

    fun setYandexOauthToken(token: String) {
        deps.uiState.update { it.copy(yandexOauthToken = token) }
        if (deps.uiState.value.syncProvider == "Yandex Disk") {
            applyProviderConfig(deps.uiState.value.syncProvider, token)
        }
        val prefs: SharedPreferences = deps.context.getSharedPreferences(
            "project_prefs",
            Context.MODE_PRIVATE,
        )
        prefs.edit {
            putString("yandex_oauth_token", token)
        }
    }

    private fun applyProviderConfig(providerName: String, token: String) {
        val provider = when (providerName) {
            "Yandex Disk" -> if (token.isNotBlank()) YandexDiskProvider(token) else null
            else -> null
        }
        deps.syncRepo.configure(provider)
    }
}