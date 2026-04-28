package com.example.markdown_editor.data.model

import android.net.Uri
import android.provider.DocumentsContract

data class Project(
    val name: String,
    val uri: Uri,           // the root directory URI from the picker
    val notesPath: String,      // notes
    val assetsPath: String,      // assets
) {
    val notesUri: Uri get() = buildChildUri(notesPath)
    val assetsUri: Uri get() = buildChildUri(assetsPath)

    fun getFileUri(path: String): Uri = buildChildUri(path)

    private fun buildChildUri(relative: String): Uri {
        val treeId = DocumentsContract.getTreeDocumentId(uri)
        val childId = if (relative.isEmpty()) treeId else "$treeId/$relative"
        return DocumentsContract.buildDocumentUriUsingTree(uri, childId)
    }
}