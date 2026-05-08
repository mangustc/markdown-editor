package com.example.markdown_editor.data.repository

import android.net.Uri
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project

interface NoteRepository {
    suspend fun createNote(
        project: Project,
        name: String? = "New Note",
        tags: List<String>? = emptyList(),
    ): Uri?

    suspend fun getNoteText(note: Note, includeFrontMatter: Boolean = true): String
    suspend fun saveNoteText(note: Note, text: String): Note
    suspend fun getNoteByUri(uri: Uri): Note
    suspend fun deleteNote(note: Note)
    suspend fun renameNote(note: Note, newName: String)
    suspend fun toggleNotePin(note: Note): String
}