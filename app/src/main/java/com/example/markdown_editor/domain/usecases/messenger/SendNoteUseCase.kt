package com.example.markdown_editor.domain.usecases.messenger

import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase
import com.example.markdown_editor.domain.usecases.notes.CreateNoteInput
import com.example.markdown_editor.domain.usecases.notes.CreateNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.SaveNoteTextInput
import com.example.markdown_editor.domain.usecases.notes.SaveNoteTextUseCase
import com.example.markdown_editor.domain.usecases.project.CopyToAssetsInput
import com.example.markdown_editor.domain.usecases.project.CopyToAssetsUseCase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SendNoteInput(
    val project: Project,
    val body: String,
    val attachments: List<Attachment>,
    val editNote: Note? = null,
)

class SendNoteUseCase(
    private val projectRepository: ProjectRepository,
    private val createNoteUseCase: CreateNoteUseCase,
    private val copyToAssetsUseCase: CopyToAssetsUseCase,
    private val saveNoteTextUseCase: SaveNoteTextUseCase,
) : UseCase<SendNoteInput, Unit> {
    override suspend fun invoke(input: SendNoteInput) {
        val isEditedNote = input.editNote != null
        val targetNote = if (isEditedNote) {
            input.editNote
        } else {
            val timestamp = DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now())
            val name = "quick-note-$timestamp"
            val tags = listOf(FrontMatter.QUICK_NOTE_TAG)
            createNoteUseCase(
                CreateNoteInput(
                    project = input.project,
                    name = name,
                    tags = tags,
                ),
            )
        }

        val baseText = projectRepository.readFile(
            project = input.project,
            relativePath = targetNote.projectFile.relativePath,
        ).decodeToString()

        val parentContent = if (isEditedNote) {
            val frontMatterEnd = run {
                if (!baseText.trimStart().startsWith("---")) return@run 0
                val lines = baseText.lines()
                val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
                if (closeIdx < 0) 0
                else lines.take(closeIdx + 2).joinToString("\n").length
            }
            baseText.substring(0, frontMatterEnd).trimEnd()
        } else {
            baseText
        }

        val attachmentLines = buildString {
            input.attachments.forEach { attachment ->
                val label = attachment.displayName
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                val firstPart = when (attachment.type) {
                    Attachment.AttachmentType.IMAGE -> "![$label]"
                    Attachment.AttachmentType.FILE -> "[$label]"
                }
                when (attachment) {
                    is Attachment.PendingAttachment -> {
                        val projectFile =
                            copyToAssetsUseCase(
                                CopyToAssetsInput(
                                    project = input.project,
                                    assetPath = attachment.fileSystemPath,
                                ),
                            )
                        append("\n$firstPart(<${projectFile.relativePath.value}>)")
                    }

                    is Attachment.ProjectAttachment -> {
                        if (isEditedNote) append("\n$firstPart(<${attachment.relativePath.value}>)")
                    }

                    is Attachment.InvalidProjectAttachment -> {}
                }
            }
        }

        val finalContent = when {
            input.body.isNotEmpty() && attachmentLines.isNotEmpty() ->
                "$parentContent\n\n${input.body}$attachmentLines"

            input.body.isNotEmpty() ->
                "$parentContent\n\n${input.body}"

            attachmentLines.isNotEmpty() ->
                "$parentContent\n$attachmentLines"

            else -> parentContent
        }


        saveNoteTextUseCase(
            SaveNoteTextInput(
                project = input.project,
                note = targetNote,
                text = finalContent,
            ),
        )
    }
}
