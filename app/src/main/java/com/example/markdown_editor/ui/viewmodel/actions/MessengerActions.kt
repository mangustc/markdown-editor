package com.example.markdown_editor.ui.viewmodel.actions

import androidx.core.net.toUri
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.model.SortBy
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.messenger.Attachment
import com.example.markdown_editor.domain.messenger.AttachmentType
import com.example.markdown_editor.ui.viewmodel.AppDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessengerActions(
    private val deps: AppDeps,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val notesPaged: Flow<PagingData<Note>> = deps.uiState
        .map { it.project }
        .distinctUntilChanged()
        .flatMapLatest { project ->
            if (project == null) return@flatMapLatest emptyFlow()
            deps.projectRepo.getNotesPaged(
                project,
                SearchQuery(tagFilters = listOf("quick-note"), sortBy = SortBy.CREATED_AT),
                includeText = true,
                includeFrontMatter = false,
            )
        }
        .cachedIn(deps.scope)

    fun onMessengerOpened(project: Project, afterUpdate: () -> Unit = {}) {
        deps.scope.launch {
            deps.projectRepo.syncDatabase(project)
            val pinnedNotes = deps.projectRepo.getNotes(
                project = project,
                SearchQuery(
                    tagFilters = listOf("quick-note", "pinned"),
                    sortBy = SortBy.CREATED_AT,
                ),
                includeText = true,
                includeFrontMatter = false,
            )
            deps.uiState.update {
                it.copy(
                    messengerPinnedNotes = pinnedNotes,
                    messengerIsLoading = false,
                )
            }
            afterUpdate()
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
        val project = deps.uiState.value.project ?: return
        val text = deps.uiState.value.messengerNewNoteText.trim()

        // Pre-checks
        val editNote = if (isEditedNote) {
            deps.uiState.value.messengerEditingNote ?: return
        } else {
            if (text.isBlank() && attachments.isEmpty()) return
            null
        }

        deps.scope.launch(Dispatchers.IO) {
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
                deps.noteRepo.getNoteByUri(uri)
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
                    when (attachment.type) {
                        AttachmentType.PENDING_IMAGE -> {
                            val path =
                                deps.projectRepo.copyToAssets(project, attachment.path.toUri())
                            append("\n![image](<$path>)")
                        }

                        AttachmentType.PENDING_FILE -> {
                            val path =
                                deps.projectRepo.copyToAssets(project, attachment.path.toUri())
                            val label = attachment.displayName
                                .replace("[", "\\[")
                                .replace("]", "\\]")
                            append("\n[$label](<$path>)")
                        }

                        AttachmentType.IMAGE -> {
                            if (isEditedNote) append("\n![image](<${attachment.relativePath}>)")
                        }

                        AttachmentType.FILE -> {
                            val label = attachment.displayName
                                .replace("[", "\\[")
                                .replace("]", "\\]")
                            if (isEditedNote) append("\n[${label}](<${attachment.relativePath}>)")
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

            withContext(Dispatchers.Main) {
                deps.uiState.update { state ->
                    state.copy(
                        messengerNewNoteText = "",
                        messengerEditingNote = if (isEditedNote) null else state.messengerEditingNote,
                    )
                }
            }
            deps.globalActions.updateNoteLists()
            afterUpdate()
        }
    }

    fun ensureLinkPreview(url: String) {
        if (deps.uiState.value.messengerLinkPreviews.containsKey(url)) return
        deps.uiState.update { it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to null)) }

        deps.scope.launch {
            val preview = deps.linkRepo.getLinkPreview(url)
            if (preview != null) {
                deps.uiState.update {
                    it.copy(messengerLinkPreviews = it.messengerLinkPreviews + (url to preview))
                }
                return@launch
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
        val uris = deps.uiState.value.messengerSelectedNotes
        val project = deps.uiState.value.project ?: return
        deps.scope.launch(Dispatchers.IO) {
            uris.forEach { u ->
                runCatching { deps.noteRepo.deleteNote(deps.noteRepo.getNoteByUri(u.toUri())) }
            }
            deps.projectRepo.syncDatabase(project)
            withContext(Dispatchers.Main) {
                clearSelection()
                deps.globalActions.updateNoteLists()
            }
        }
    }

    suspend fun getSelectedNotesText(): String = withContext(Dispatchers.IO) {
        deps.uiState.value.messengerSelectedNotes.mapNotNull { u ->
            runCatching {
                val note = deps.noteRepo.getNoteByUri(u.toUri())
                val text = deps.noteRepo.getNoteText(note, includeFrontMatter = false)

                MarkdownParser.stripAttachments(text, MarkdownParser.parse(text))
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }.joinToString("\n\n")
    }
}