package com.example.markdown_editor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.markdown_editor.data.repository.LinkPreviewRepository
import com.example.markdown_editor.data.repository.NoteRepository
import com.example.markdown_editor.data.repository.ProjectRepository
import com.example.markdown_editor.data.repository.SettingsRepository
import com.example.markdown_editor.data.sync.SyncRepository
import com.example.markdown_editor.domain.messenger.Attachment
import com.example.markdown_editor.ui.viewmodel.actions.DrawerActions
import com.example.markdown_editor.ui.viewmodel.actions.EditorActions
import com.example.markdown_editor.ui.viewmodel.actions.MessengerActions
import com.example.markdown_editor.ui.viewmodel.actions.ProjectActions
import com.example.markdown_editor.ui.viewmodel.actions.SettingsActions
import com.example.markdown_editor.ui.viewmodel.events.AppEvent
import com.example.markdown_editor.ui.viewmodel.events.ClipboardEvent
import com.example.markdown_editor.ui.viewmodel.events.FocusEvent
import com.example.markdown_editor.ui.viewmodel.events.NavigationEvent
import com.example.markdown_editor.ui.viewmodel.events.NotificationEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    application: Application,
    private val projectRepo: ProjectRepository,
    private val noteRepo: NoteRepository,
    private val linkRepo: LinkPreviewRepository,
    private val syncRepo: SyncRepository,
    private val settingsRepo: SettingsRepository,
) : AndroidViewModel(application), AppGlobalActions {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val deps by lazy {
        AppDeps(
            scope = viewModelScope,
            projectRepo = projectRepo,
            noteRepo = noteRepo,
            linkRepo = linkRepo,
            syncRepo = syncRepo,
            settingsRepo = settingsRepo,
            uiState = _uiState,
            globalActions = this,
        )
    }

    val project = ProjectActions(deps)
    val settings = SettingsActions(deps)

    init {
        project.loadSavedProject()
    }

    val drawer = DrawerActions(deps)
    val editor = EditorActions(deps)
    val messenger = MessengerActions(deps)

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val _notificationEvents = Channel<NotificationEvent>(Channel.BUFFERED)
    val notificationEvents = _notificationEvents.receiveAsFlow()

    private val _clipboardEvents = Channel<ClipboardEvent>(Channel.BUFFERED)
    val clipboardEvents = _clipboardEvents.receiveAsFlow()

    private val _focusEvents = Channel<FocusEvent>(Channel.BUFFERED)
    val focusEvents = _focusEvents.receiveAsFlow()

    override fun onEvent(event: AppEvent) {
        deps.scope.launch {
            when (event) {
                is NavigationEvent -> _navigationEvents.send(event)
                is NotificationEvent -> _notificationEvents.send(event)
                is ClipboardEvent -> _clipboardEvents.send(event)
                is FocusEvent -> _focusEvents.send(event)
            }
        }
    }

    override suspend fun updateNoteLists() {
        val project = _uiState.value.project ?: return
        projectRepo.syncDatabase(project)
        _uiState.update { it.copy(allProjectTags = projectRepo.getAllTags()) }
        messenger.onMessengerOpened(project)
    }

    fun onShareIntent(text: String?, attachments: List<Attachment>) {
        _uiState.update { state ->
            state.copy(
                messengerNewNoteText = if (!text.isNullOrEmpty()) text else state.messengerNewNoteText,
                pendingIntentAttachments = attachments,
            )
        }
    }

    fun consumePendingIntentAttachments(): List<Attachment> {
        val pending = _uiState.value.pendingIntentAttachments
        if (pending.isNotEmpty()) {
            _uiState.update { it.copy(pendingIntentAttachments = emptyList()) }
        }
        return pending
    }
}