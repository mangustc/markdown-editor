package com.example.markdown_editor.domain.viewmodel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.data.model.FrontMatter
import com.example.markdown_editor.data.model.FrontMatterValue
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.domain.editor.EditorEvent
import com.example.markdown_editor.domain.navigation.SearchEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class EditorActions(
    private val deps: AppDeps,
) {
    val state = TextFieldState()
    val linkSearchState = TextFieldState()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkSearchResultsPaged: Flow<PagingData<Note>> = combine(
        deps.uiState.map { it.project }.distinctUntilChanged(),
        snapshotFlow { linkSearchState.text },
    ) { project, text -> project to text.toString() }
        .flatMapLatest { (project, queryStr) ->
            if (project == null) return@flatMapLatest emptyFlow()
            deps.projectRepo.syncDatabase(project)
            val parsedInit = SearchQuery.parse(queryStr.trim())
            val parsed = parsedInit.copy(
                negatedTagFilters = parsedInit.negatedTagFilters + "quick-note",
                pinnedFirst = true,
            )
            deps.projectRepo.getNotesPaged(project, parsed)
        }
        .cachedIn(deps.scope)

    fun onLinkSearchEvent(event: SearchEvent) {
        event.execute(linkSearchState)
    }

    fun showLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = true) }
    }

    fun dismissLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = false) }
        linkSearchState.setTextAndPlaceCursorAtEnd("")
    }

    fun insertNoteLink(note: Note) {
        val syntax = "[${note.name}](<${note.name}.md>)"
        editorInsertSyntax(syntax, syntax.length)
        dismissLinkNoteDialog()
    }

    init {
        deps.scope.launch {
            snapshotFlow { state.text }
                .debounce(2_000)
                .collect { editorOnSave() }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun editorOnEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.InsertSyntax -> editorInsertSyntax(event.syntax, event.cursorOffset)
            is EditorEvent.AttachPhoto -> editorHandleAttachPhoto(event)
            is EditorEvent.AttachFile -> editorHandleAttachFile(event)
            is EditorEvent.Undo -> {
                state.undoState.undo()
            }

            is EditorEvent.Redo -> {
                state.undoState.redo()
            }
        }
    }

    private fun editorInsertSyntax(syntax: String, cursorOffset: Int) {
        state.edit {
            val start = selection.start
            replace(start, start, syntax)
            placeCursorAfterCharAt(start + cursorOffset - 1)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun editorOnNoteOpened(noteUriString: String) {
        deps.scope.launch(Dispatchers.IO) {
            try {
                val note = deps.noteRepo.getNoteByUri(noteUriString.toUri())
                val text = deps.noteRepo.getNoteText(note)
                val (frontMatter, body) = FrontMatter.splitFromContent(text)
                withContext(Dispatchers.Main) {
                    deps.uiState.update {
                        it.copy(
                            activeNote = note,
                            editorFrontMatter = frontMatter,
                        )
                    }
                    state.edit {
                        replace(0, length, body)
                    }
                    state.undoState.clearHistory()
                }
            } catch (_: Exception) {
                deps.globalActions.goBack()
            }
        }
    }

    fun updateFmKey(oldKey: String, newKey: String) {
        if (oldKey == newKey || newKey.isBlank()) return
        deps.uiState.update {
            it.copy(
                editorFrontMatter = it.editorFrontMatter?.withRenamedKey(
                    oldKey,
                    newKey,
                ),
            )
        }
        editorOnSave()
    }

    fun updateFmValue(key: String, value: String) {
        deps.uiState.update {
            it.copy(
                editorFrontMatter = it.editorFrontMatter?.withField(
                    key,
                    FrontMatterValue.Scalar(value),
                ),
            )
        }
        editorOnSave()
    }

    fun addFmProperty() {
        val fm = deps.uiState.value.editorFrontMatter ?: FrontMatter.Empty
        var newKey = "newProperty"
        var count = 1
        while (fm.fields.containsKey(newKey)) {
            newKey = "newProperty$count"
            count++
        }
        deps.uiState.update {
            it.copy(
                editorFrontMatter = fm.withField(
                    newKey,
                    FrontMatterValue.Scalar(""),
                ),
            )
        }
        editorOnSave()
    }

    fun addFmTag(tag: String) {
        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withTag(tag)) }
        editorOnSave()
    }

    fun removeFmTag(tag: String) {
        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withoutTag(tag)) }
        editorOnSave()
    }

    fun removeFmProperty(key: String) {
        if (key == "createdAt" || key == "tags") return

        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withoutField(key)) }
        editorOnSave()
    }

    private fun editorHandleAttachPhoto(event: EditorEvent.AttachPhoto) {
        val project = deps.uiState.value.project ?: return
        deps.scope.launch(Dispatchers.IO) {
            val relativePath =
                deps.projectRepo.copyToAssets(project = project, assetUri = event.uri)
            val markdown = "![image](<$relativePath>)"
            withContext(Dispatchers.Main) { editorInsertSyntax(markdown, 0) }
        }
    }

    private fun editorHandleAttachFile(event: EditorEvent.AttachFile) {
        val project = deps.uiState.value.project ?: return
        deps.scope.launch(Dispatchers.IO) {
            val relativePath =
                deps.projectRepo.copyToAssets(project = project, assetUri = event.uri)
            val label = event.displayName ?: relativePath.substringAfterLast("/")
            val markdown = "[$label](<$relativePath>)"
            withContext(Dispatchers.Main) { editorInsertSyntax(markdown, 0) }
        }
    }

    fun editorOnSave() {
        val project = deps.uiState.value.project ?: return
        val note = deps.uiState.value.activeNote ?: return
        val bodyText = state.text.toString()
        val fm = deps.uiState.value.editorFrontMatter

        val textToSave = if (fm != null && fm.fields.isNotEmpty()) {
            "$fm\n$bodyText"
        } else {
            bodyText

        }

        deps.scope.launch(Dispatchers.IO) {
            deps.noteRepo.saveNoteText(note, textToSave)
            deps.projectRepo.syncDatabase(project)
            deps.globalActions.updateNoteLists()
            deps.uiState.update { it.copy(editorSavedVersion = it.editorVersion) }
        }
    }
}

