package com.example.markdown_editor.domain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.repository.LinkPreviewRepositoryImpl
import com.example.markdown_editor.data.repository.NoteRepositoryImpl
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.data.repository.SettingsRepositoryImpl
import com.example.markdown_editor.data.sync.SyncRepositoryImpl
import com.example.markdown_editor.domain.messenger.Attachment
import com.example.markdown_editor.domain.viewmodel.actions.DrawerActions
import com.example.markdown_editor.domain.viewmodel.actions.EditorActions
import com.example.markdown_editor.domain.viewmodel.actions.MessengerActions
import com.example.markdown_editor.domain.viewmodel.actions.ProjectActions
import com.example.markdown_editor.domain.viewmodel.actions.SettingsActions
import com.example.markdown_editor.domain.viewmodel.events.AppEvent
import com.example.markdown_editor.domain.viewmodel.events.NavigationEvent
import com.example.markdown_editor.domain.viewmodel.events.NotificationEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application), AppGlobalActions {
    private val db = Room.databaseBuilder(
        application,
        NoteDb::class.java, "database-notes",
    )
        .fallbackToDestructiveMigration(true)
        .build()

    private val projectRepo = ProjectRepositoryImpl(
        context = application,
        noteDao = db.noteDao(),
    )
    private val noteRepo = NoteRepositoryImpl(context = application)
    private val linkRepo = LinkPreviewRepositoryImpl(linkPreviewDao = db.linkPreviewDao())
    private val syncRepo = SyncRepositoryImpl(context = application)
    private val settingsRepo = SettingsRepositoryImpl(context = application)

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val deps by lazy {
        AppDeps(
            context = application,
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
    private val _toastEvents = Channel<NotificationEvent>(Channel.BUFFERED)
    val toastEvents = _toastEvents.receiveAsFlow()

    override fun onEvent(event: AppEvent) {
        deps.scope.launch {
            when (event) {
                is NavigationEvent -> _navigationEvents.send(event)
                is NotificationEvent -> _toastEvents.send(event)
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