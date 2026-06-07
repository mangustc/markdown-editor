package com.example.markdown_editor.data.project

import com.example.markdown_editor.data.database.NoteEntity
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath

fun NoteEntity.toNote(project: Project, includeText: Boolean): Note = Note(
    name = name,
    projectFile = ProjectFile(
        fileSystemPath = FileSystemPath(uri),
        relativePath = project.notesRelativePath.appendRelativePath(
            RelativePath("${name}.md"),
        ),
    ),
    lastModified = lastModified,
    createdAt = createdAt,
    body = if (includeText) body else null,
    tags = if (tags.isNotEmpty()) tags.split(" ") else emptyList(),
)