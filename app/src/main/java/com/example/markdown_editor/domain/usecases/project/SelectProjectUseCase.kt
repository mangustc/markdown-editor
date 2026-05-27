package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.data.project.SettingsRepository
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.usecases.UseCase

data class SelectProjectInput(
    val projectPath: FileSystemPath,
)

class SelectProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
) : UseCase<SelectProjectInput, Project> {
    override suspend fun invoke(input: SelectProjectInput): Project {
        settingsRepository.setProjectPath(input.projectPath)
        return projectRepository.buildProject(input.projectPath)
    }
}
