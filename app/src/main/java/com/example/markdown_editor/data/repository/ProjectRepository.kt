package com.example.markdown_editor.data.repository

import android.net.Uri
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun buildProject(rootUri: Uri, name: String): Project
    fun getNotes(project: Project): Flow<List<Note>>
    suspend fun saveProject(project: Project)
    suspend fun loadSavedProject(): Project?
    suspend fun createNote(project: Project, name: String? = "New Note", tags: List<String>? = emptyList()): Uri?
    suspend fun searchNotes(project: Project, query: SearchQuery): List<Note>
    suspend fun getNoteText(note: Note, includeFrontMatter: Boolean = true): String
    suspend fun saveNoteText(note: Note, text: String): Note
    suspend fun getNoteByUri(uri: Uri): Note
}