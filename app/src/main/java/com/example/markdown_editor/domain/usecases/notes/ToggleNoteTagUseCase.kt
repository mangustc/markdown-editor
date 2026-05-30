package com.example.markdown_editor.domain.usecases.notes

import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class ToggleNoteTagInput(
    val project: Project,
    val note: Note,
    val tag: String,
)

class ToggleNoteTagUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<ToggleNoteTagInput, FrontMatter> {
    override suspend fun invoke(input: ToggleNoteTagInput): FrontMatter {
        val fullText =
            projectRepository.readFile(input.project, input.note.projectFile.relativePath)
                .decodeToString()
        val (frontMatter, body) = FrontMatter.splitFromContent(fullText)

        val updatedFrontMatter = if (input.tag in frontMatter.tags) {
            frontMatter.withoutTag(input.tag)
        } else {
            frontMatter.withTag(input.tag)
        }

        projectRepository.writeFile(
            project = input.project,
            relativePath = input.note.projectFile.relativePath,
            byteArray = "${updatedFrontMatter}\n$body".toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )

        return updatedFrontMatter
    }
}
