package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.usecases.UseCase

data class GetAllTagsInput(
    val project: Project,
)

class GetAllTagsUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetAllTagsInput, List<String>> {
    override suspend fun invoke(input: GetAllTagsInput): List<String> {
        return projectRepository.getAllTags(input.project)
    }
}
