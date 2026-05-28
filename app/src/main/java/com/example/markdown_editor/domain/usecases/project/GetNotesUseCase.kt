package com.example.markdown_editor.domain.usecases.project

import androidx.paging.PagingData
import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.domain.QUICK_NOTE_TAG
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.domain.usecases.FlowUseCase
import kotlinx.coroutines.flow.Flow

data class GetNotesInput(
    val project: Project,
    val searchQueryString: String,
)

class GetNotesUseCase(
    private val projectRepository: ProjectRepository,
) : FlowUseCase<GetNotesInput, PagingData<Note>> {
    override fun invoke(input: GetNotesInput): Flow<PagingData<Note>> {
        val parsedInit = SearchQuery.parse(input.searchQueryString.trim())
        val parsed = parsedInit.copy(
            negatedTagFilters = parsedInit.negatedTagFilters + QUICK_NOTE_TAG,
            pinnedFirst = true,
        )
        return projectRepository.getNotesPaged(input.project, parsed)
    }
}
