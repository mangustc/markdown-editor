package com.example.markdown_editor.ui.viewmodel.actions

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.MessageBody
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.usecases.messenger.GetMessagesInput
import com.example.markdown_editor.domain.usecases.messenger.GetMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesInput
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesUseCase
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessengerActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val getMessagesUseCase: GetMessagesUseCase by inject()
    private val getPinnedMessagesUseCase: GetPinnedMessagesUseCase by inject()

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

            val editNote = if (isEditedNote) {
                deps.uiState.value.messengerEditingNote ?: return@launch
            } else {
                if (text.isBlank() && attachments.isEmpty()) return@launch
                null
            }

            val targetNote = if (isEditedNote) {
                editNote!!
            } else {
                val timestamp = DateTimeFormatter
                    .ofPattern("yyyyMMdd_HHmmss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now())
                val name = "quick-note-$timestamp"
                val tags = listOf("quick-note")
                val uri = deps.noteRepo.createNote(project, name, tags) ?: return@launch
                deps.noteRepo.getNoteByFileSystemPath(uri)
            }

            val baseText = deps.noteRepo.getNoteText(targetNote, includeFrontMatter = true)

            val parentContent = if (isEditedNote) {
                val frontMatterEnd = run {
                    if (!baseText.trimStart().startsWith("---")) return@run 0
                    val lines = baseText.lines()
                    val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
                    if (closeIdx < 0) 0
                    else lines.take(closeIdx + 2).joinToString("\n").length
                }
                baseText.substring(0, frontMatterEnd).trimEnd()
            } else {
                baseText
            }

            val attachmentLines = buildString {
                attachments.forEach { attachment ->
                    val label = attachment.displayName
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                    val firstPart = when (attachment.type) {
                        Attachment.AttachmentType.IMAGE -> "![$label]"
                        Attachment.AttachmentType.FILE -> "[$label]"
                    }
                    when (attachment) {
                        is Attachment.PendingAttachment -> {
                            val projectFile =
                                deps.projectRepo.copyToAssets(project, attachment.fileSystemPath)
                            append("\n$firstPart(<${projectFile.relativePath.value}>)")
                        }

                        is Attachment.ProjectAttachment -> {
                            if (isEditedNote) append("\n$firstPart(<${attachment.relativePath.value}>)")
                        }
                    }
                }
            }

            val finalContent = when {
                text.isNotEmpty() && attachmentLines.isNotEmpty() ->
                    "$parentContent\n\n$text$attachmentLines"

                text.isNotEmpty() ->
                    "$parentContent\n\n$text"

                attachmentLines.isNotEmpty() ->
                    "$parentContent\n$attachmentLines"

                else -> parentContent
            }

            deps.noteRepo.saveNoteText(targetNote, finalContent)
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
            val preview = deps.linkRepo.getLinkPreview(url)
            if (preview != null) {
                deps.uiState.update {
                    it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to preview))
                }
            }
        }
    }

    fun toggleNoteSelection(uriString: String) {
        deps.uiState.update {
            val sel = it.messengerSelectedNotes
            it.copy(messengerSelectedNotes = if (sel.contains(uriString)) sel - uriString else sel + uriString)
        }
    }

    fun clearSelection() {
        deps.uiState.update { it.copy(messengerSelectedNotes = emptySet()) }
    }

    fun deleteSelectedNotes() {
        deps.scope.launch {
            val uris = deps.uiState.value.messengerSelectedNotes
            val project = deps.uiState.value.project ?: return@launch
            uris.forEach { u ->
                runCatching {
                    deps.noteRepo.deleteNote(
                        deps.noteRepo.getNoteByFileSystemPath(
                            FileSystemPath(u),
                        ),
                    )
                }
            }
            deps.projectRepo.syncDatabase(project)
            clearSelection()
            deps.globalActions.updateNoteLists()
        }
    }

    fun copySelectedNotesText() {
        deps.scope.launch {
            val text = deps.uiState.value.messengerSelectedNotes.mapNotNull { u ->
                runCatching {
                    val note = deps.noteRepo.getNoteByFileSystemPath(FileSystemPath(u))
                    val text = deps.noteRepo.getNoteText(note, includeFrontMatter = false)

                    MarkdownParser.stripAttachments(text, MarkdownParser.parse(text))
                }.getOrNull()?.takeIf { it.isNotBlank() }
            }.joinToString("\n\n")
            deps.globalActions.onEvent(ClipboardEvent.Copy(text))
            clearSelection()
        }
    }
}