package com.example.markdown_editor.ui.viewmodel.actions

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.usecases.linkPreview.GetLinkPreviewInput
import com.example.markdown_editor.domain.usecases.linkPreview.GetLinkPreviewUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetMessagesInput
import com.example.markdown_editor.domain.usecases.messenger.GetMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesInput
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.SendNoteInput
import com.example.markdown_editor.domain.usecases.messenger.SendNoteUseCase
import com.example.markdown_editor.domain.usecases.project.DeleteNoteInput
import com.example.markdown_editor.domain.usecases.project.DeleteNoteUseCase
import com.example.markdown_editor.ui.viewmodel.AppDeps
import com.example.markdown_editor.ui.viewmodel.events.ClipboardEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessengerActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val getMessagesUseCase: GetMessagesUseCase by inject()
    private val getPinnedMessagesUseCase: GetPinnedMessagesUseCase by inject()
    private val deleteNoteUseCase: DeleteNoteUseCase by inject()
    private val getLinkPreviewUseCase: GetLinkPreviewUseCase by inject()
    private val sendNoteUseCase: SendNoteUseCase by inject()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesPaged: Flow<PagingData<MessageBody>> = deps.uiState
        .map { it.project }
        .distinctUntilChanged()
        .flatMapLatest { project ->
            if (project == null) return@flatMapLatest emptyFlow()
            getMessagesUseCase(GetMessagesInput(project))
        }
        .cachedIn(deps.scope)

    suspend fun updateMessages() {
        val project = deps.uiState.value.project ?: return
        val pinnedMessages = getPinnedMessagesUseCase(
            GetPinnedMessagesInput(
                project = project,
            ),
        )
        deps.uiState.update {
            it.copy(
                messengerPinnedMessages = pinnedMessages,
                messengerIsLoading = false,
            )
        }
    }

    fun onNewNoteTextChanged(text: String) {
        deps.uiState.update { it.copy(messengerNewNoteText = text) }
    }

    fun startEditNote(note: Note, parsedText: String) {
        deps.uiState.update {
            it.copy(
                messengerEditingNote = note,
                messengerNewNoteText = parsedText,
            )
        }
    }

    fun cancelEditNote() {
        deps.uiState.update {
            it.copy(messengerEditingNote = null, messengerNewNoteText = "")
        }
    }

    fun onSendNote(
        isEditedNote: Boolean,
        attachments: List<Attachment> = emptyList(),
        afterUpdate: () -> Unit = {},
    ) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val text = deps.uiState.value.messengerNewNoteText.trim()

            sendNoteUseCase(
                SendNoteInput(
                    project = project,
                    body = text,
                    attachments = attachments,
                    editNote = if (isEditedNote) {
                        deps.uiState.value.messengerEditingNote ?: return@launch
                    } else {
                        if (text.isBlank() && attachments.isEmpty()) return@launch
                        null
                    },
                ),
            )

            deps.uiState.update { state ->
                state.copy(
                    messengerNewNoteText = "",
                    messengerEditingNote = if (isEditedNote) null else state.messengerEditingNote,
                )
            }
            deps.globalActions.updateNoteLists()
            afterUpdate()
        }
    }

    fun ensureLinkPreview(url: String) {
        deps.scope.launch {
            if (deps.uiState.value.messengerLinkPreviews.containsKey(url)) return@launch
            val preview = getLinkPreviewUseCase(
                GetLinkPreviewInput(
                    url = url,
                ),
            )
            if (preview != null) {
                deps.uiState.update {
                    it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to preview))
                }
            }
        }
    }

    fun toggleNoteSelection(message: MessageBody) {
        deps.uiState.update {
            val sel = it.messengerSelectedNotes
            it.copy(messengerSelectedNotes = if (sel.contains(message)) sel - message else sel + message)
        }
    }

    fun clearSelection() {
        deps.uiState.update { it.copy(messengerSelectedNotes = emptySet()) }
    }

    fun deleteSelectedNotes() {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val messages = deps.uiState.value.messengerSelectedNotes
            messages.forEach { message ->
                runCatching {
                    deleteNoteUseCase(
                        DeleteNoteInput(
                            project = project,
                            note = message.note,
                        ),
                    )
                }
            }
            clearSelection()
            deps.globalActions.updateNoteLists()
        }
    }

    fun copySelectedNotesText() {
        deps.scope.launch {
            val text = deps.uiState.value.messengerSelectedNotes.joinToString("\n\n") { message ->
                message.text
            }
            deps.globalActions.onEvent(ClipboardEvent.Copy(text))
            clearSelection()
        }
    }
}