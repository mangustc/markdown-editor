package com.example.markdown_editor.ui.viewmodel.actions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.snapshotFlow
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SpanInfo
import com.example.markdown_editor.domain.usecases.editor.ApplyEditorEventInput
import com.example.markdown_editor.domain.usecases.editor.ApplyEditorEventUseCase
import com.example.markdown_editor.domain.usecases.editor.EditorEvent
import com.example.markdown_editor.domain.usecases.project.GetNoteInput
import com.example.markdown_editor.domain.usecases.project.GetNoteUseCase
import com.example.markdown_editor.domain.usecases.project.GetNotesInput
import com.example.markdown_editor.domain.usecases.project.GetNotesUseCase
import com.example.markdown_editor.domain.usecases.project.GetProjectFileInput
import com.example.markdown_editor.domain.usecases.project.GetProjectFileUseCase
import com.example.markdown_editor.domain.usecases.project.GetRealSpanInfoLinkTypeInput
import com.example.markdown_editor.domain.usecases.project.GetRealSpanInfoLinkTypeUseCase
import com.example.markdown_editor.domain.usecases.project.SaveNoteTextInput
import com.example.markdown_editor.domain.usecases.project.SaveNoteTextUseCase
import com.example.markdown_editor.domain.usecases.search.ApplySearchEventInput
import com.example.markdown_editor.domain.usecases.search.ApplySearchEventUseCase
import com.example.markdown_editor.domain.usecases.search.SearchEvent
import com.example.markdown_editor.ui.components.ComposeTextState
import com.example.markdown_editor.ui.viewmodel.AppDeps
import com.example.markdown_editor.ui.viewmodel.events.NavigationEvent
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(FlowPreview::class)
class EditorActions(
    private val deps: AppDeps,
) : KoinComponent {
    private val getNotesUseCase: GetNotesUseCase by inject()
    private val getRealSpanInfoLinkTypeUseCase: GetRealSpanInfoLinkTypeUseCase by inject()
    private val getNoteUseCase: GetNoteUseCase by inject()
    private val getProjectFileUseCase: GetProjectFileUseCase by inject()
    private val saveNoteTextUseCase: SaveNoteTextUseCase by inject()
    private val applyEditorEventUseCase: ApplyEditorEventUseCase by inject()
    private val applySearchEventUseCase: ApplySearchEventUseCase by inject()

    val state = ComposeTextState()
    val linkSearchState = ComposeTextState()

    @OptIn(ExperimentalCoroutinesApi::class)
    val linkSearchResultsPaged: Flow<PagingData<Note>> = combine(
        deps.uiState.map { it.project }.distinctUntilChanged(),
        snapshotFlow { linkSearchState.text },
    ) { project, text -> project to text.toString() }
        .flatMapLatest { (project, queryStr) ->
            if (project == null) return@flatMapLatest emptyFlow()
            getNotesUseCase(
                GetNotesInput(
                    project = project,
                    searchQueryString = queryStr,
                ),
            )
        }
        .cachedIn(deps.scope)

    fun onLinkSearchEvent(event: SearchEvent) {
        deps.scope.launch {
            applySearchEventUseCase(
                ApplySearchEventInput(
                    event = event,
                    state = linkSearchState,
                ),
            )
        }
    }

    fun showLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = true) }
    }

    fun dismissLinkNoteDialog() {
        deps.uiState.update { it.copy(isLinkNoteDialogVisible = false) }
        linkSearchState.setTextAndPlaceCursorAtEnd("")
    }

    init {
        deps.scope.launch {
            snapshotFlow { state.text }
                .debounce(2_000)
                .collect { onSave() }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onEvent(event: EditorEvent) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            when (event) {
                is EditorEvent.InsertNoteLink -> {
                    applyEditorEventUseCase(
                        ApplyEditorEventInput(
                            project = project,
                            state = state,
                            event = event,
                        ),
                    )
                    dismissLinkNoteDialog()
                }

                else -> {
                    applyEditorEventUseCase(
                        ApplyEditorEventInput(
                            project = project,
                            state = state,
                            event = event,
                        ),
                    )
                }
            }
        }
    }

    fun openLink(span: SpanInfo.Link) {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val realLinkType = getRealSpanInfoLinkTypeUseCase(
                GetRealSpanInfoLinkTypeInput(
                    project = project,
                    span = span,
                ),
            ) ?: return@launch
            when (realLinkType) {
                SpanInfo.Link.LinkType.NOTE -> {
                    val note = getNoteUseCase(
                        GetNoteInput(
                            project = project,
                            relativePath = RelativePath(span.payload),
                        ),
                    ) ?: return@launch
                    deps.globalActions.onEvent(NavigationEvent.GoToEditor(note = note))
                }

                SpanInfo.Link.LinkType.FILE -> {
                    val projectFile = getProjectFileUseCase(
                        GetProjectFileInput(
                            project = project,
                            relativePath = RelativePath(span.payload),
                        ),
                    ) ?: return@launch
                    deps.globalActions.onEvent(NavigationEvent.OpenFile(projectFile.fileSystemPath))
                }

                SpanInfo.Link.LinkType.HTTP -> {
                    deps.globalActions.onEvent(NavigationEvent.OpenUrl(span.payload))
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun onNoteOpened(notePath: RelativePath) {
        deps.scope.launch {
            try {
                val project = deps.uiState.value.project ?: throw Exception()
                val note = getNoteUseCase(
                    GetNoteInput(
                        project = project,
                        relativePath = notePath,
                        includeText = true,
                        includeFrontMatter = true,
                    ),
                )
                val (frontMatter, body) = FrontMatter.splitFromContent(
                    note?.body ?: throw Exception(),
                )
                deps.uiState.update {
                    it.copy(
                        activeNote = note,
                        editorFrontMatter = frontMatter,
                        isViewingMode = true,
                    )
                }
                state.edit {
                    replace(0, length, body)
                }
                state.undoState.clearHistory()
            } catch (_: Exception) {
                deps.globalActions.onEvent(NavigationEvent.GoBack)
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
        onSave()
    }

    fun updateFmValue(key: String, value: String) {
        deps.uiState.update {
            it.copy(
                editorFrontMatter = it.editorFrontMatter?.withField(
                    key,
                    FrontMatter.FrontMatterValue.Scalar(value),
                ),
            )
        }
        onSave()
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
                    FrontMatter.FrontMatterValue.Scalar(""),
                ),
            )
        }
        onSave()
    }

    fun addFmTag(tag: String) {
        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withTag(tag)) }
        onSave()
    }

    fun removeFmTag(tag: String) {
        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withoutTag(tag)) }
        onSave()
    }

    fun removeFmProperty(key: String) {
        deps.uiState.update { it.copy(editorFrontMatter = it.editorFrontMatter?.withoutField(key)) }
        onSave()
    }

    fun toggleViewingMode() {
        deps.uiState.update { it.copy(isViewingMode = !it.isViewingMode) }
    }

    fun onSave() {
        deps.scope.launch {
            val project = deps.uiState.value.project ?: return@launch
            val note = deps.uiState.value.activeNote ?: return@launch
            val bodyText = state.text.toString()
            val fm = deps.uiState.value.editorFrontMatter

            val textToSave = if (fm != null && fm.fields.isNotEmpty()) {
                "$fm\n$bodyText"
            } else {
                bodyText
            }

            saveNoteTextUseCase(
                SaveNoteTextInput(
                    project = project,
                    note = note,
                    text = textToSave,
                ),
            )
            deps.globalActions.updateNoteLists()
            deps.uiState.update { it.copy(editorSavedVersion = it.editorVersion) }
        }
    }
}