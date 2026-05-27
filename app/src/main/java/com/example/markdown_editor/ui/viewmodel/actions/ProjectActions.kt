package com.example.markdown_editor.ui.viewmodel.actions

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.usecases.project.LoadSavedProjectUseCase
import com.example.markdown_editor.domain.usecases.project.SelectProjectInput
import com.example.markdown_editor.domain.usecases.project.SelectProjectUseCase
import com.example.markdown_editor.domain.usecases.sync.SyncProjectInput
import com.example.markdown_editor.domain.usecases.sync.SyncProjectUseCase
import com.example.markdown_editor.ui.viewmodel.AppDeps
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProjectActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val syncProjectUseCase: SyncProjectUseCase by inject()
    private val loadSavedProjectUseCase: LoadSavedProjectUseCase by inject()
    private val selectProjectUseCase: SelectProjectUseCase by inject()

    fun onProjectSelected(projectPath: FileSystemPath) {
        deps.scope.launch {
            val project = selectProjectUseCase(
                SelectProjectInput(
                    projectPath = projectPath,
                ),
            )
            val settings = deps.settingsRepo.getSettings(project)
            deps.uiState.update { it.copy(project = project, settings = settings) }
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
        }
    }

    fun loadSavedProject() {
        deps.scope.launch {
            val project = loadSavedProjectUseCase(Unit)
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
            syncProjectUseCase(
                SyncProjectInput(
                    project = project,
                    settings = settings,
                ),
            )
            deps.uiState.update { it.copy(isSyncInProgress = false) }
            deps.globalActions.updateNoteLists()
        }
    }
}