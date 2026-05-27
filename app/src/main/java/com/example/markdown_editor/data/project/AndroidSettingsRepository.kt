package com.example.markdown_editor.data.project

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings
import kotlinx.serialization.json.Json

class AndroidSettingsRepository(
    private val context: Context,
) : SettingsRepository {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    override fun setSettings(
        project: Project,
        settings: Settings,
    ): Settings {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "${KEY_PREFIX_PROJECT}${project.name}",
            Context.MODE_PRIVATE,
        )
        val settingsJson = json.encodeToString(settings)
        prefs.edit {
            putString(KEY_SETTINGS, settingsJson)
        }
        return settings
    }

    override fun getSettings(project: Project): Settings {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "${KEY_PREFIX_PROJECT}${project.name}",
            Context.MODE_PRIVATE,
        )
        val settingsJson = prefs.getString(KEY_SETTINGS, null)
        val settings = runCatching {
            if (settingsJson == null) return@runCatching null
            json.decodeFromString<Settings>(settingsJson)
        }.getOrNull() ?: Settings.EMPTY
        return settings
    }

    override fun setProjectPath(path: FileSystemPath) {
        val prefs: SharedPreferences = context.getSharedPreferences(
            KEY_PROJECT_URI,
            Context.MODE_PRIVATE,
        )
        val projectUri = path.value.toUri()
        context.contentResolver.takePersistableUriPermission(
            projectUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit {
            putString(KEY_PROJECT_URI, projectUri.toString())
        }
    }

    override fun getProjectPath(): FileSystemPath? {
        val prefs: SharedPreferences = context.getSharedPreferences(
            KEY_PROJECT_URI,
            Context.MODE_PRIVATE,
        )
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return null
        return FileSystemPath(uriString)
    }

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_PREFIX_PROJECT = "settings_"
        private const val KEY_PROJECT_URI = "project_uri"
    }
}
