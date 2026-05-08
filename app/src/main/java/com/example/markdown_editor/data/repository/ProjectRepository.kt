package com.example.markdown_editor.data.repository

import android.net.Uri
import androidx.paging.PagingData
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    suspend fun getNotes(
        project: Project,
        query: SearchQuery = SearchQuery(),
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): List<Note>

    fun getNotesPaged(
        project: Project,
        query: SearchQuery = SearchQuery(),
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): Flow<PagingData<Note>>

    fun buildProject(rootUri: Uri, name: String): Project
    suspend fun saveProject(project: Project)
    suspend fun loadSavedProject(): Project?
    suspend fun syncDatabase(project: Project)
    suspend fun copyToAssets(project: Project, assetUri: Uri): String
}