package com.example.markdown_editor.domain.usecases.notes

import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.repositories.ProjectRepository
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
    private val getNoteUseCase: GetNoteUseCase,
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
