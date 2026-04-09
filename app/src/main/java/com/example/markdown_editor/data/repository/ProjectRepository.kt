package com.example.markdown_editor.data.repository

import android.net.Uri
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getNotes(project: Project): Flow<List<Note>>
    suspend fun saveProject(project: Project)
    suspend fun loadSavedProject(): Project?
    suspend fun createNote(project: Project, name: String): Uri?
}