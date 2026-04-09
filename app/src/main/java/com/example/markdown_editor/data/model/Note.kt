package com.example.markdown_editor.data.model

import android.net.Uri

data class Note(
    val name: String,       // filename without .md extension
    val uri: Uri,           // full URI to the file
    val lastModified: Long
)