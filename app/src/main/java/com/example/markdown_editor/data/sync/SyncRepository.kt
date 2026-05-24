package com.example.markdown_editor.data.sync

import android.content.Context
import com.example.markdown_editor.data.model.Project

interface SyncRepository {
    val activeProvider: SyncProvider?

    suspend fun sync(project: Project): SyncResult
    fun configure(provider: SyncProvider?)
}

class SyncRepositoryImpl(
    private val context: Context,
) : SyncRepository {

    override var activeProvider: SyncProvider? = null
        private set

    private var engine: SyncEngine? = null

    override fun configure(provider: SyncProvider?) {
        activeProvider = provider
        engine = if (provider != null) SyncEngine(context, provider) else null
    }

    override suspend fun sync(project: Project): SyncResult {
        val e = engine ?: return SyncResult(
            actions = emptyList(),
            newManifest = SyncManifest.EMPTY,
            errors = listOf("No sync provider configured"),
        )
        return e.sync(project)
    }
}