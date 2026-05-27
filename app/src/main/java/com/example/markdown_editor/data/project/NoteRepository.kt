package com.example.markdown_editor.data.project

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project

interface NoteRepository {
    suspend fun createNote(
        project: Project,
        name: String? = "New Note",
        tags: List<String>? = emptyList(),
    ): FileSystemPath?

    suspend fun getNoteText(note: Note, includeFrontMatter: Boolean = true): String
    suspend fun saveNoteText(note: Note, text: String): Note
    suspend fun getNoteByFileSystemPath(path: FileSystemPath): Note
    suspend fun deleteNote(note: Note)
    suspend fun renameNote(note: Note, newName: String)
    suspend fun toggleNotePin(note: Note): String
}