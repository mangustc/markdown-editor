package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class GetProjectFileInput(
    val project: Project,
    val relativePath: RelativePath,
)

class GetProjectFileUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetProjectFileInput, ProjectFile?> {
    override suspend fun invoke(input: GetProjectFileInput): ProjectFile? {
        return projectRepository.getProjectFile(input.project, input.relativePath)
    }
}
