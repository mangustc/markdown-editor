package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
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
        ) ?: throw Exception("failed to rename file")
        val note = getNoteUseCase(
            GetNoteInput(
                project = input.project,
                relativePath = newProjectFile.relativePath,
                includeText = input.includeText,
                includeFrontMatter = input.includeFrontMatter,
            ),
        ) ?: throw Exception("failed to get created file")

        return note
    }
}
