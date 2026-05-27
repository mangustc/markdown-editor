package com.example.markdown_editor.data.repository

import android.net.Uri
import androidx.paging.PagingData
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
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

    fun buildProject(rootUri: Uri): Project
    suspend fun saveProject(project: Project)
    suspend fun loadSavedProject(): Project?
    suspend fun syncDatabase(project: Project)
    suspend fun copyToAssets(project: Project, assetUri: Uri): String
    suspend fun getAllTags(): List<String>
    suspend fun writeFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        overwrite: Boolean = true,
        createParents: Boolean = true,
    ): ProjectFile?

    suspend fun deleteFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
    )

    suspend fun readFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
    ): ByteArray?

    suspend fun getProjectFilesList(
        project: com.example.markdown_editor.domain.models.Project,
    ): List<ProjectFile>
}