package com.example.markdown_editor.domain.usecases.messenger

import androidx.paging.PagingData
import androidx.paging.map
import com.example.markdown_editor.domain.QUICK_NOTE_TAG
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.FlowUseCase
import com.example.markdown_editor.domain.usecases.project.GetProjectFileInput
import com.example.markdown_editor.domain.usecases.project.GetProjectFileUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GetMessagesInput(
    val project: Project,
)

class GetMessagesUseCase(
    private val projectRepository: ProjectRepository,
    private val getProjectFileUseCase: GetProjectFileUseCase,
) : FlowUseCase<GetMessagesInput, PagingData<MessageBody>> {
    override fun invoke(input: GetMessagesInput): Flow<PagingData<MessageBody>> {
        val notesFlow: Flow<PagingData<Note>> = projectRepository.getNotesPaged(
            input.project,
            SearchQuery(
                tagFilters = listOf(QUICK_NOTE_TAG),
                sortBy = SearchQuery.SortBy.CREATED_AT,
            ),
            includeText = true,
            includeFrontMatter = false,
        )

        val mappedFlow: Flow<PagingData<MessageBody>> = notesFlow.map { pagingData ->
            pagingData.map { note ->
                note.toMessageBody {
                    getProjectFileUseCase(
                        GetProjectFileInput(
                            project = input.project,
                            relativePath = it,
                        ),
                    )
                }
            }
        }

        return mappedFlow
    }
}
