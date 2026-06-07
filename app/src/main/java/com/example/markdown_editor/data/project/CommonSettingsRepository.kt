package com.example.markdown_editor.data.project

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.repositories.PlatformPathHandler
import com.example.markdown_editor.domain.repositories.SettingsRepository
import com.russhwolf.settings.Settings.Factory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class CommonSettingsRepository(
    private val settingsFactory: Factory,
    private val platformPathHandler: PlatformPathHandler,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override suspend fun setSettings(
        project: Project,
        settings: Settings,
    ): Settings = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create("${KEY_PREFIX_PROJECT}${project.name}")
        val settingsJson = json.encodeToString(settings)
        prefs.putString(KEY_SETTINGS, settingsJson)
        settings
    }

    override suspend fun getSettings(project: Project): Settings = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create("${KEY_PREFIX_PROJECT}${project.name}")
        val settingsJson = prefs.getStringOrNull(KEY_SETTINGS)
        val settings = runCatching {
            if (settingsJson == null) return@runCatching null
            json.decodeFromString<Settings>(settingsJson)
        }.getOrNull() ?: Settings.EMPTY
        settings
    }

    override suspend fun setProjectPath(path: FileSystemPath) = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create(KEY_PROJECT_URI)

        platformPathHandler.takePersistablePathPermission(path)

        prefs.putString(KEY_PROJECT_URI, path.value)
    }

    override suspend fun getProjectPath(): FileSystemPath? = withContext(Dispatchers.IO) {
        val prefs = settingsFactory.create(KEY_PROJECT_URI)
        val uriString = prefs.getStringOrNull(KEY_PROJECT_URI) ?: return@withContext null
        FileSystemPath(uriString)
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_PREFIX_PROJECT = "settings_"
        private const val KEY_PROJECT_URI = "project_uri"
    }
}
