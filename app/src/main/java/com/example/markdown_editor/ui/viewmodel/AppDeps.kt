package com.example.markdown_editor.ui.viewmodel

import com.example.markdown_editor.domain.repositories.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class AppDeps(
    val scope: CoroutineScope,
    val noteRepo: NoteRepository,
    val uiState: MutableStateFlow<AppUiState>,
    val globalActions: AppGlobalActions,
)