package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.usecases.UseCase

data class DeleteNoteInput(
    val project: Project,
    val note: Note,
)

class DeleteNoteUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<DeleteNoteInput, Unit> {
    override suspend fun invoke(input: DeleteNoteInput) {
        projectRepository.deleteFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
        )
    }
}
