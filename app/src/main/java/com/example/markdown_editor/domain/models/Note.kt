package com.example.markdown_editor.domain.models

data class Note(
    val name: String,
    val projectFile: ProjectFile,
    val lastModified: Long,
    val createdAt: Long? = null,
    val tags: List<String>? = null,
    val body: String? = null,
)
