package com.example.markdown_editor.ui.viewmodel.actions

import android.net.Uri
import android.util.Log
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.usecases.sync.SyncAuthException
import com.example.markdown_editor.domain.usecases.sync.SyncLocalIoException
import com.example.markdown_editor.domain.usecases.sync.SyncNetworkException
import com.example.markdown_editor.domain.usecases.sync.SyncProjectInput
import com.example.markdown_editor.domain.usecases.sync.SyncProjectUseCase
import com.example.markdown_editor.domain.usecases.sync.SyncQuotaException
import com.example.markdown_editor.domain.usecases.sync.SyncServerException
import com.example.markdown_editor.domain.usecases.sync.SyncStateException
import com.example.markdown_editor.ui.viewmodel.AppDeps
import com.example.markdown_editor.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProjectActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val syncProjectUseCase: SyncProjectUseCase by inject()

    fun onProjectSelected(uri: Uri) {
        deps.scope.launch {
            val project = deps.projectRepo.buildProject(FileSystemPath(uri.toString()))
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
                syncProjectUseCase(
                    SyncProjectInput(
                        project = project,
                        settings = settings,
                    ),
                )
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