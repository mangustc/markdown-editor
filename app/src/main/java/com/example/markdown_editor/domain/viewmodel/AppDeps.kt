package com.example.markdown_editor.domain.viewmodel

import android.content.Context
import com.example.markdown_editor.data.repository.LinkPreviewRepository
import com.example.markdown_editor.data.repository.NoteRepository
import com.example.markdown_editor.data.repository.ProjectRepository
import com.example.markdown_editor.data.repository.SettingsRepository
import com.example.markdown_editor.data.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class AppDeps(
    val scope: CoroutineScope,
    val projectRepo: ProjectRepository,
    val noteRepo: NoteRepository,
    val linkRepo: LinkPreviewRepository,
    val syncRepo: SyncRepository,
    val settingsRepo: SettingsRepository,
    val uiState: MutableStateFlow<AppUiState>,
    val globalActions: AppGlobalActions,
)