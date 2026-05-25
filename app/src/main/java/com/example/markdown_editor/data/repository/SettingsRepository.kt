package com.example.markdown_editor.data.repository

import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.Settings

interface SettingsRepository {
    fun setSettings(project: Project, settings: Settings): Settings
    fun getSettings(project: Project): Settings
}