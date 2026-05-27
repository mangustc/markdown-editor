package com.example.markdown_editor.ui.viewmodel

import com.example.markdown_editor.data.linkPreview.LinkPreviewRepository
import com.example.markdown_editor.data.project.NoteRepository
import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.data.project.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class AppDeps(
    val scope: CoroutineScope,
    val projectRepo: ProjectRepository,
    val noteRepo: NoteRepository,
    val linkRepo: LinkPreviewRepository,
    val settingsRepo: SettingsRepository,
    val uiState: MutableStateFlow<AppUiState>,
    val globalActions: AppGlobalActions,
)