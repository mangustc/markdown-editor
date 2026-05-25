package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.Settings
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
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

    companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_PREFIX_PROJECT = "settings_"
    }
}
