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