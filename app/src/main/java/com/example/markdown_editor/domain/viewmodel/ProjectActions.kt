package com.example.markdown_editor.domain.viewmodel

import android.net.Uri
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
        val project = deps.uiState.value.project ?: return
        deps.scope.launch {
            deps.syncRepo.sync(project)
            deps.globalActions.updateNoteLists()
        }
    }

}