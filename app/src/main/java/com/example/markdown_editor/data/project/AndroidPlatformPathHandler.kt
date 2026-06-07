package com.example.markdown_editor.data.project

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.repositories.PlatformPathHandler

class AndroidPlatformPathHandler(
    private val context: Context,
) : PlatformPathHandler {
    override fun takePersistablePathPermission(path: FileSystemPath) {
        val projectUri = path.value.toUri()
        context.contentResolver.takePersistableUriPermission(
            projectUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}