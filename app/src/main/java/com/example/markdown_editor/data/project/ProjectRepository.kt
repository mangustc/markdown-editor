package com.example.markdown_editor.data.project

import androidx.paging.PagingData
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SearchQuery
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

    suspend fun buildProject(projectPath: FileSystemPath): Project
    suspend fun syncDatabase(project: Project)
    suspend fun copyToAssets(project: Project, assetPath: FileSystemPath): ProjectFile
    suspend fun getAllTags(): List<String>
    suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        overwrite: Boolean = true,
        createParents: Boolean = true,
    ): ProjectFile?

    suspend fun deleteFile(
        project: Project,
        relativePath: RelativePath,
    )

    suspend fun readFile(
        project: Project,
        relativePath: RelativePath,
    ): ByteArray?

    suspend fun getProjectFilesList(
        project: Project,
    ): List<ProjectFile>

    suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile?
}