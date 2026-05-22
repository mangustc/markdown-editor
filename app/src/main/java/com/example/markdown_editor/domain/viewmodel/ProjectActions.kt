package com.example.markdown_editor.domain.viewmodel

import android.net.Uri
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProjectActions(
    private val deps: AppDeps,
) {
    fun onProjectSelected(uri: Uri) {
        deps.scope.launch {
            val name = uri.lastPathSegment?.substringAfterLast(":") ?: "Project"
            val project = deps.projectRepo.buildProject(uri, name)
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
}