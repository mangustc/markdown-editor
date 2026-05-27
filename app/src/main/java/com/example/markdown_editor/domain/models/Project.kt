package com.example.markdown_editor.domain.models

data class Project(
    val name: String,
    val rootFileSystemPath: FileSystemPath,
    val notesRelativePath: RelativePath,
    val assetsRelativePath: RelativePath,
) {
    companion object {
        val DefaultNotesRelativePath = RelativePath("notes")
        val DefaultAssetsRelativePath = RelativePath("assets")
    }
}
