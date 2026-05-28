package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class SyncDatabaseInput(
    val project: Project,
)

class SyncDatabaseUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<SyncDatabaseInput, Unit> {
    override suspend fun invoke(input: SyncDatabaseInput) {
        projectRepository.syncDatabase(input.project)
    }
}
