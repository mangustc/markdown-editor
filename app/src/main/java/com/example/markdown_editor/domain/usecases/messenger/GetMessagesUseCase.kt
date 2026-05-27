package com.example.markdown_editor.domain.usecases.messenger

import androidx.paging.PagingData
import androidx.paging.map
import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.domain.models.SpanInfo
import com.example.markdown_editor.domain.usecases.UseCase
import com.example.markdown_editor.domain.usecases.UseCaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GetMessagesInput(
    val project: Project,
)

private val URL_PATTERN = Regex("""https?://[^\s<>"')]+""")

class GetMessagesUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetMessagesInput, Flow<PagingData<MessageBody>>> {
    override suspend fun invoke(input: GetMessagesInput): UseCaseResult<Flow<PagingData<MessageBody>>> {
        val notesFlow: Flow<PagingData<Note>> = projectRepository.getNotesPaged(
            input.project,
            SearchQuery(tagFilters = listOf("quick-note"), sortBy = SearchQuery.SortBy.CREATED_AT),
            includeText = true,
            includeFrontMatter = false,
        )

        val mappedFlow: Flow<PagingData<MessageBody>> = notesFlow.map { pagingData ->
            pagingData.map { note ->
                getMessageBody(input.project, note)
            }
        }

        return UseCaseResult.Success(mappedFlow)
    }

    private suspend fun getMessageBody(project: Project, note: Note): MessageBody {
        val body = note.body ?: ""
        val spans = MarkdownParser.parse(body)

        val imageAttachments = spans.filterIsInstance<SpanInfo.Image>().mapNotNull { span ->
            val projectFile = projectRepository.getProjectFile(project, RelativePath(span.payload))
                ?: return@mapNotNull null
            Attachment.ProjectAttachment(
                type = Attachment.AttachmentType.IMAGE,
                fileSystemPath = projectFile.fileSystemPath,
                relativePath = projectFile.relativePath,
                displayName = span.payload,
            )
        }

        val fileAttachments = spans.filterIsInstance<SpanInfo.Link>().mapNotNull { span ->
            val projectFile = projectRepository.getProjectFile(project, RelativePath(span.payload))
                ?: return@mapNotNull null
            Attachment.ProjectAttachment(
                type = Attachment.AttachmentType.FILE,
                fileSystemPath = projectFile.fileSystemPath,
                relativePath = projectFile.relativePath,
                displayName = span.payload,
            )
        }

        val text = MarkdownParser.stripAttachments(body, spans).ifBlank { "" }
        val links = URL_PATTERN.findAll(text)

        return MessageBody(
            text = text,
            attachments = imageAttachments + fileAttachments,
            links = links,
            note = note,
        )

    }
}
