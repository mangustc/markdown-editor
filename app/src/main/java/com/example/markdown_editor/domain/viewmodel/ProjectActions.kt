package com.example.markdown_editor.domain.viewmodel

import android.net.Uri
import android.util.Log
import com.example.markdown_editor.data.sync.SyncAuthException
import com.example.markdown_editor.data.sync.SyncLocalIoException
import com.example.markdown_editor.data.sync.SyncNetworkException
import com.example.markdown_editor.data.sync.SyncQuotaException
import com.example.markdown_editor.data.sync.SyncServerException
import com.example.markdown_editor.data.sync.SyncStateException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectActions(
    private val deps: AppDeps,
) {
    fun onProjectSelected(uri: Uri) {
        deps.scope.launch {
            val project = deps.projectRepo.buildProject(uri)
            deps.projectRepo.saveProject(project)
            deps.uiState.update { it.copy(project = project) }
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
        }
    }

    fun loadSavedProject() {
        deps.scope.launch {
            val project = deps.projectRepo.loadSavedProject()
            if (project != null) {
                deps.uiState.update { it.copy(project = project) }
                deps.globalActions.updateNoteLists()
            } else {
                deps.uiState.update { it.copy(messengerIsLoading = false) }
            }
        }
    }

    fun syncNow() {
        if (deps.uiState.value.isSyncInProgress) return
        val project = deps.uiState.value.project ?: return
        deps.scope.launch {
            deps.uiState.update { it.copy(isSyncInProgress = true) }
            try {
                deps.syncRepo.sync(project)
            } catch (_: SyncAuthException) {
                deps.globalActions.showToast(NotificationEvent.SyncAuthException)
            } catch (_: SyncNetworkException) {
                deps.globalActions.showToast(NotificationEvent.SyncNetworkException)
            } catch (_: SyncServerException) {
                deps.globalActions.showToast(NotificationEvent.SyncServerException)
            } catch (_: SyncLocalIoException) {
                deps.globalActions.showToast(NotificationEvent.SyncLocalIoException)
            } catch (_: SyncStateException) {
                deps.globalActions.showToast(NotificationEvent.SyncStateException)
            } catch (_: SyncQuotaException) {
                deps.globalActions.showToast(NotificationEvent.SyncQuotaException)
            } catch (e: Exception) {
                Log.e("debug", e.toString())
                deps.globalActions.showToast(
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