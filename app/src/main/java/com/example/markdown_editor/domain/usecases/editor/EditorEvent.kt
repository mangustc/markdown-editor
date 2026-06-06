package com.example.markdown_editor.domain.usecases.editor

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note

sealed interface EditorEvent {
    data class AttachPhoto(val path: FileSystemPath) : EditorEvent
    data class AttachFile(
        val path: FileSystemPath,
        val displayName: String? = null,
    ) : EditorEvent

    data class InsertNoteLink(val note: Note) : EditorEvent

    data object Bold : EditorEvent
    data object Italic : EditorEvent
    data object Code : EditorEvent
    data object Undo : EditorEvent
    data object Redo : EditorEvent
}