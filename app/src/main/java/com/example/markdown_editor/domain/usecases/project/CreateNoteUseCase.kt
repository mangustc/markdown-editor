package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.usecases.UseCase
import java.time.Instant

data class CreateNoteInput(
    val project: Project,
    val name: String,
    val tags: List<String> = emptyList(),
    val initialText: String = "",
    val includeText: Boolean = false,
    val includeFrontMatter: Boolean = false,
)

class CreateNoteUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<CreateNoteInput, Note> {
    override suspend fun invoke(input: CreateNoteInput): Note {
        val isoDate = Instant.now().toString()
        var frontMatterBuilder = "---\ncreatedAt: $isoDate"
        frontMatterBuilder += "\ntags:"
        if (input.tags.isNotEmpty()) {
            input.tags.forEach { tag -> frontMatterBuilder += "\n- $tag" }
        }
        val initialContent = "$frontMatterBuilder\n---\n${input.initialText}"

        val newProjectFile = projectRepository.writeFile(
            project = input.project,
            relativePath = input.project.notesRelativePath.appendRelativePath(RelativePath("${input.name}.md")),
            byteArray = initialContent.toByteArray(),
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.AUTO_RENAME,
        ) ?: throw Exception("could not get created file")
        val note = projectRepository.getNote(
            project = input.project,
            relativePath = newProjectFile.relativePath,
            includeText = input.includeText,
            includeFrontMatter = input.includeFrontMatter,
        ) ?: throw Exception("could not get created file")

        return note
    }
}
