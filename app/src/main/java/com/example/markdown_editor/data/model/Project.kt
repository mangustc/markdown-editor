package com.example.markdown_editor.data.model

import android.net.Uri

data class Project(
    val name: String,
    val uri: Uri,           // the root directory URI from the picker
    val notesUri: Uri,      // uri/notes/
    val assetsUri: Uri      // uri/assets/
)