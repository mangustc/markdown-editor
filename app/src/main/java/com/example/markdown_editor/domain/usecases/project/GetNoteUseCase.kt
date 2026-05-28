package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.usecases.UseCase

data class GetNoteInput(
    val project: Project,
    val relativePath: RelativePath,
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class GetNoteUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetNoteInput, Note> {
    override suspend fun invoke(input: GetNoteInput): Note {
        val note = projectRepository.getNote(
            project = input.project,
            relativePath = input.relativePath,
            includeText = input.includeText,
            includeFrontMatter = input.includeFrontMatter,
        ) ?: throw Exception("could not get file")

        return note
    }
}
