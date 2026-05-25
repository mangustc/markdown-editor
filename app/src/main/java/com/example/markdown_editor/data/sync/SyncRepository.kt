package com.example.markdown_editor.data.sync

import android.content.Context
import com.example.markdown_editor.data.model.Project

interface SyncRepository {
    suspend fun sync(project: Project, syncProvider: SyncProvider?): SyncResult
}

class SyncRepositoryImpl(
    private val context: Context,
) : SyncRepository {
    override suspend fun sync(project: Project, syncProvider: SyncProvider?): SyncResult {
        val engine = if (syncProvider != null) SyncEngine(context, syncProvider) else null
        val e = engine ?: return SyncResult(
            actions = emptyList(),
            newManifest = SyncManifest.EMPTY,
            errors = listOf("No sync provider configured"),
        )
        return e.sync(project)
    }
}