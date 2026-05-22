package com.example.markdown_editor.domain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.markdown_editor.data.database.NoteDb
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.repository.LinkPreviewRepository
import com.example.markdown_editor.data.repository.LinkPreviewRepositoryImpl
import com.example.markdown_editor.data.repository.NoteRepository
import com.example.markdown_editor.data.repository.NoteRepositoryImpl
import com.example.markdown_editor.data.repository.ProjectRepository
import com.example.markdown_editor.data.repository.ProjectRepositoryImpl
import com.example.markdown_editor.domain.messenger.Attachment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppDeps(
    val scope: CoroutineScope,
    val projectRepo: ProjectRepository,
    val noteRepo: NoteRepository,
    val linkRepo: LinkPreviewRepository,
    val uiState: MutableStateFlow<AppUiState>,
    val globalActions: AppGlobalActions,
)

interface AppGlobalActions {
    fun updateNoteLists(
        afterUpdateSearch: () -> Unit = {},
        afterUpdateMessenger: () -> Unit = {},
    )

    fun goToEditor(note: Note)
    fun goBack()
    fun openDrawer()
    fun closeDrawer()
}

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
    private val noteRepo = NoteRepositoryImpl(
        context = application,
    )
    private val linkRepo = LinkPreviewRepositoryImpl(
        linkPreviewDao = db.linkPreviewDao(),
    )

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val deps by lazy {
        AppDeps(
            scope = viewModelScope,
            projectRepo = projectRepo,
            noteRepo = noteRepo,
            linkRepo = linkRepo,
            uiState = _uiState,
            globalActions = this,
        )
    }

    val project = ProjectActions(deps)

    init {
        project.loadSavedProject()
    }

    val drawer = DrawerActions(deps)
    val editor = EditorActions(deps)
    val messenger = MessengerActions(deps)

    sealed class NavigationEvent {
        data class GoToEditor(val note: Note) : NavigationEvent()
        object GoBack : NavigationEvent()
        object OpenDrawer : NavigationEvent()
        object CloseDrawer : NavigationEvent()
    }

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    override fun goToEditor(note: Note) {
        deps.scope.launch { _navigationEvents.send(NavigationEvent.GoToEditor(note)) }
    }

    override fun goBack() {
        deps.scope.launch { _navigationEvents.send(NavigationEvent.GoBack) }
    }

    override fun openDrawer() {
        deps.scope.launch { _navigationEvents.send(NavigationEvent.OpenDrawer) }
    }

    override fun closeDrawer() {
        deps.scope.launch { _navigationEvents.send(NavigationEvent.CloseDrawer) }
    }

    override fun updateNoteLists(afterUpdateSearch: () -> Unit, afterUpdateMessenger: () -> Unit) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            projectRepo.syncDatabase(project)
            _uiState.update { it.copy(allProjectTags = projectRepo.getAllTags()) }
            afterUpdateSearch()
        }
        messenger.onMessengerOpened(project, afterUpdate = afterUpdateMessenger)
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
