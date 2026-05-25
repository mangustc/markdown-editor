package com.example.markdown_editor.data.sync

data class SyncResult(
    val actions: List<SyncFileAction>,
    val newManifest: SyncManifest,
    val errors: List<String> = emptyList(),
) {
    val hasErrors get() = errors.isNotEmpty()
}
