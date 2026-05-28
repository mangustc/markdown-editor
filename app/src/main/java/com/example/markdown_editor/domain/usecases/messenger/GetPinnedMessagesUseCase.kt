package com.example.markdown_editor.domain.usecases.messenger

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.PINNED_TAG
import com.example.markdown_editor.domain.QUICK_NOTE_TAG
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.domain.usecases.UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GetPinnedMessagesInput(
    val project: Project,
)

class GetPinnedMessagesUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetPinnedMessagesInput, List<MessageBody>> {
    override suspend fun invoke(input: GetPinnedMessagesInput): List<MessageBody> =
        withContext(Dispatchers.Default) {
            projectRepository.getNotes(
                input.project,
                SearchQuery(
                    tagFilters = listOf(QUICK_NOTE_TAG, PINNED_TAG),
                    sortBy = SearchQuery.SortBy.CREATED_AT,
                ),
                includeText = true,
                includeFrontMatter = false,
            ).map { note ->
                MessageBody.parse(
                    note = note,
                    getProjectFile = { projectRepository.getProjectFile(input.project, it) },
                )
            }
        }
}
