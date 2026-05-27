package com.example.markdown_editor.data.project

import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings

interface SettingsRepository {
    fun setSettings(project: Project, settings: Settings): Settings
    fun getSettings(project: Project): Settings
}