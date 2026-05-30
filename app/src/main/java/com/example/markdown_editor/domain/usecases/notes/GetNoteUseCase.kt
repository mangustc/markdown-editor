package com.example.markdown_editor.domain.usecases.notes

import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.repositories.ProjectRepository
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
        )

        return note
    }
}
