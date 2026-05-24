package com.example.markdown_editor.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class SyncManifest(
    val lastSyncTimestamp: Long,
    val files: Map<String, String> = emptyMap(),
) {
    companion object {
        const val MANIFEST_PATH = ".sync_manifest.json"
        val EMPTY = SyncManifest(lastSyncTimestamp = 0L)
    }
}

sealed class SyncFileAction {
    data class Upload(val relativePath: String) : SyncFileAction()
    data class Download(val relativePath: String) : SyncFileAction()
    data class DeleteLocal(val relativePath: String) : SyncFileAction()
    data class DeleteRemote(val relativePath: String) : SyncFileAction()

    data class ConflictUpload(val relativePath: String) : SyncFileAction()
    data class NoOp(val relativePath: String) : SyncFileAction()
}