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
    suspend fun createNote(project: Project, name: String): Uri?
    suspend fun searchNotes(project: Project, query: SearchQuery): List<Note>
}