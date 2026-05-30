package com.example.markdown_editor.domain.usecases.notes

import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class RenameNoteInput(
    val project: Project,
    val note: Note,
    val newName: String,
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class RenameNoteUseCase(
    private val projectRepository: ProjectRepository,
    private val getNoteUseCase: GetNoteUseCase,
) : UseCase<RenameNoteInput, Note> {
    override suspend fun invoke(input: RenameNoteInput): Note {
        val newProjectFile = projectRepository.moveFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            newRelativePath = input.note.projectFile.relativePath.dirRelativePath.appendRelativePath(
                RelativePath("${input.newName}.md"),
            ),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.AUTO_RENAME,
        )
        val note = getNoteUseCase(
            GetNoteInput(
                project = input.project,
                relativePath = newProjectFile.relativePath,
                includeText = input.includeText,
                includeFrontMatter = input.includeFrontMatter,
            ),
        )

        return note
    }
}
