package com.example.markdown_editor.data.project

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings

interface SettingsRepository {
    suspend fun setSettings(project: Project, settings: Settings): Settings
    suspend fun getSettings(project: Project): Settings
    suspend fun setProjectPath(path: FileSystemPath)
    suspend fun getProjectPath(): FileSystemPath?
}