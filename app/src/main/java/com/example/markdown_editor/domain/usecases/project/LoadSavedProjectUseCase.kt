package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.data.project.SettingsRepository
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.usecases.UseCase

class LoadSavedProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
) : UseCase<Unit, Project?> {
    override suspend fun invoke(input: Unit): Project? {
        val savedProjectPath = settingsRepository.getProjectPath() ?: return null
        return projectRepository.buildProject(savedProjectPath)
    }
}
