package com.example.markdown_editor.domain.usecases.notes

import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class SaveNoteTextInput(
    val project: Project,
    val note: Note,
    val text: String,
)

class SaveNoteTextUseCase(
    val projectRepository: ProjectRepository,
) : UseCase<SaveNoteTextInput, Unit> {
    override suspend fun invoke(input: SaveNoteTextInput) {
        projectRepository.writeFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            byteArray = input.text.toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )
    }
}
