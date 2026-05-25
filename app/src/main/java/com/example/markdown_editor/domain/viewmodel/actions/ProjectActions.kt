package com.example.markdown_editor.domain.viewmodel.actions

import android.net.Uri
import android.util.Log
import com.example.markdown_editor.data.sync.SyncAuthException
import com.example.markdown_editor.data.sync.SyncLocalIoException
import com.example.markdown_editor.data.sync.SyncNetworkException
import com.example.markdown_editor.data.sync.SyncQuotaException
import com.example.markdown_editor.data.sync.SyncServerException
import com.example.markdown_editor.data.sync.SyncStateException
import com.example.markdown_editor.data.sync.ValidSyncProvider
import com.example.markdown_editor.data.sync.YandexDiskProvider
import com.example.markdown_editor.domain.viewmodel.AppDeps
import com.example.markdown_editor.domain.viewmodel.events.NotificationEvent
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectActions(
    private val deps: AppDeps,
) {
    fun onProjectSelected(uri: Uri) {
        deps.scope.launch {
            val project = deps.projectRepo.buildProject(uri)
            deps.projectRepo.saveProject(project)
            val settings = deps.settingsRepo.getSettings(project)
            deps.uiState.update { it.copy(project = project, settings = settings) }
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
        }
    }

    fun loadSavedProject() {
        deps.scope.launch {
            val project = deps.projectRepo.loadSavedProject()
            if (project != null) {
                val settings = deps.settingsRepo.getSettings(project)
                deps.uiState.update { it.copy(project = project, settings = settings) }
                deps.globalActions.updateNoteLists()
            } else {
                deps.uiState.update { it.copy(messengerIsLoading = false) }
            }
        }
    }

    fun syncNow() {
        if (deps.uiState.value.isSyncInProgress) return
        val project = deps.uiState.value.project ?: return
        val settings = deps.uiState.value.settings ?: return
        deps.scope.launch {
            deps.uiState.update { it.copy(isSyncInProgress = true) }
            try {
                val syncProvider = when (settings.syncProvider) {
                    ValidSyncProvider.YANDEX -> {
                        YandexDiskProvider(oauthToken = settings.yandexOauthToken)
                    }

                    ValidSyncProvider.NONE -> {
                        deps.globalActions.onEvent(NotificationEvent.SyncServiceIsNone)
                        deps.uiState.update { it.copy(isSyncInProgress = false) }
                        return@launch
                    }
                }
                deps.syncRepo.sync(project, syncProvider)
            } catch (_: SyncAuthException) {
                deps.globalActions.onEvent(NotificationEvent.SyncAuthException)
            } catch (_: SyncNetworkException) {
                deps.globalActions.onEvent(NotificationEvent.SyncNetworkException)
            } catch (_: SyncServerException) {
                deps.globalActions.onEvent(NotificationEvent.SyncServerException)
            } catch (_: SyncLocalIoException) {
                deps.globalActions.onEvent(NotificationEvent.SyncLocalIoException)
            } catch (_: SyncStateException) {
                deps.globalActions.onEvent(NotificationEvent.SyncStateException)
            } catch (_: SyncQuotaException) {
                deps.globalActions.onEvent(NotificationEvent.SyncQuotaException)
            } catch (e: Exception) {
                Log.e("debug", e.toString())
                deps.globalActions.onEvent(
                    NotificationEvent.CustomMessage(
                        message = e.localizedMessage ?: "Unknown error",
                    ),
                )
            }
            deps.uiState.update { it.copy(isSyncInProgress = false) }
            deps.globalActions.updateNoteLists()
        }
    }
}