package com.example.markdown_editor.data.repository

import android.net.Uri
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery

interface ProjectRepository {
    fun buildProject(rootUri: Uri, name: String): Project
    suspend fun saveProject(project: Project)
    suspend fun loadSavedProject(): Project?
    suspend fun createNote(
        project: Project,
        name: String? = "New Note",
        tags: List<String>? = emptyList(),
    ): Uri?

    suspend fun getNotes(
        project: Project,
        query: SearchQuery = SearchQuery(),
        includeText: Boolean = false,
        includeFrontMatter: Boolean = true,
    ): List<Note>

    suspend fun getNoteText(note: Note, includeFrontMatter: Boolean = true): String
    suspend fun saveNoteText(note: Note, text: String): Note
    suspend fun getNoteByUri(uri: Uri): Note
    suspend fun syncDatabase(project: Project)
    suspend fun copyToAssets(project: Project, assetUri: Uri): String
    suspend fun deleteNote(note: Note)
    suspend fun renameNote(note: Note, newName: String)

    suspend fun getCachedLinkPreview(url: String): LinkPreview?
    suspend fun saveLinkPreview(preview: LinkPreview)
}