package com.example.markdown_editor.domain.repositories

import com.example.markdown_editor.domain.models.FileSystemPath

interface PlatformPathHandler {
    fun takePersistablePathPermission(path: FileSystemPath)
}