package com.example.markdown_editor.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class AppDeps(
    val scope: CoroutineScope,
    val uiState: MutableStateFlow<AppUiState>,
    val globalActions: AppGlobalActions,
)