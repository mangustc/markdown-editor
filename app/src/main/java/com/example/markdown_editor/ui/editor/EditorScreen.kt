package com.example.markdown_editor.ui.editor

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.example.markdown_editor.R
import com.example.markdown_editor.domain.CREATED_AT_FIELD
import com.example.markdown_editor.domain.TAGS_FIELD
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.models.SpanInfo
import com.example.markdown_editor.ui.components.NoteDrawerItem
import com.example.markdown_editor.ui.components.NoteSearchBar
import com.example.markdown_editor.ui.components.TooltipIconButton
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import com.example.markdown_editor.ui.viewmodel.events.EditorEvent

data class EditorLayoutState(
    val layout: TextLayoutResult,
    val imageSpans: List<SpanInfo.Image>,
)

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class,
)
@Composable
fun EditorScreen(
    viewModel: AppViewModel,
    noteRelativePath: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val isViewingMode by rememberUpdatedState(uiState.isViewingMode)

    LaunchedEffect(noteRelativePath) {
        viewModel.editor.onNoteOpened(RelativePath(noteRelativePath))
    }

    val editorSpans by remember {
        derivedStateOf {
            MarkdownParser.parse(viewModel.editor.state.text.toString())
        }
    }
    var layoutState by remember { mutableStateOf<EditorLayoutState?>(null) }
    var editorWidth by remember { mutableIntStateOf(0) }
    var imageAspectRatios by remember { mutableStateOf(mapOf<String, Float>()) }
    val onLayoutChange = remember(editorSpans) {
        { layoutResult: TextLayoutResult? ->
            if (layoutResult != null) {
                val images = editorSpans.filterIsInstance<SpanInfo.Image>()
                layoutState = EditorLayoutState(layoutResult, images)
                if (editorWidth != layoutResult.size.width) {
                    editorWidth = layoutResult.size.width
                }
            }
        }
    }

    val linkColor = MaterialTheme.colorScheme.primary
    val dimmedTextColor = MaterialTheme.colorScheme.primaryFixedDim
    val outputTransformation = remember {
        MarkdownOutputTransformation(
            state = viewModel.editor.state,
            density = density,
            widthProvider = { editorWidth },
            spansProvider = { editorSpans },
            ratiosProvider = { imageAspectRatios },
            linkColor = linkColor,
            dimmedTextColor = dimmedTextColor,
            isViewingModeProvider = { isViewingMode },
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.editor.onEvent(EditorEvent.AttachPhoto(uri = uri))
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.editor.onEvent(EditorEvent.AttachFile(uri = uri))
        }
    }


    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var toolbarHeightDp by remember { mutableStateOf(0.dp) }
    val imeBottom = WindowInsets.ime.getBottom(density)
    LaunchedEffect(viewModel.editor.state.selection, layoutState, toolbarHeightDp, imeBottom) {
        if (uiState.isViewingMode) return@LaunchedEffect
        val layoutResult = layoutState?.layout ?: return@LaunchedEffect
        val selection = viewModel.editor.state.selection
        if (selection.collapsed) {
            val cursor = selection.start.coerceIn(0, layoutResult.layoutInput.text.length)
            val cursorRect = layoutResult.getCursorRect(cursor)
            val lineIndex = layoutResult.getLineForOffset(cursor)
            val lineHeight =
                layoutResult.getLineBottom(lineIndex) - layoutResult.getLineTop(lineIndex)
            val toolbarHeightPx = with(density) { toolbarHeightDp.toPx() }

            bringIntoViewRequester.bringIntoView(
                androidx.compose.ui.geometry.Rect(
                    left = cursorRect.left,
                    top = cursorRect.top,
                    right = cursorRect.right,
                    bottom = cursorRect.bottom + toolbarHeightPx + lineHeight,
                ),
            )
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        val toolbarScrollState = rememberScrollState()
        if (!isViewingMode) {
            HorizontalFloatingToolbar(
                expanded = true,
                expandedShadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = -ScreenOffset)
                    .zIndex(1f)
                    .onSizeChanged {
                        toolbarHeightDp = with(density) { it.height.toDp() + ScreenOffset * 3 }
                    },
            ) {
                Row(modifier = Modifier.horizontalScroll(toolbarScrollState)) {
                    TooltipIconButton(
                        onClick = { viewModel.editor.onEvent(EditorEvent.Undo) },
                        icon = Icons.AutoMirrored.Filled.Undo,
                        tooltip = stringResource(R.string.undo),
                        enabled = viewModel.editor.state.undoState.canUndo,
                    )
                    TooltipIconButton(
                        onClick = { viewModel.editor.onEvent(EditorEvent.Redo) },
                        icon = Icons.AutoMirrored.Filled.Redo,
                        tooltip = stringResource(R.string.redo),
                        enabled = viewModel.editor.state.undoState.canRedo,
                    )
                    TooltipIconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        icon = Icons.Default.Image,
                        tooltip = stringResource(R.string.attach_photo),
                    )
                    TooltipIconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        icon = Icons.Default.AttachFile,
                        tooltip = stringResource(R.string.attach_file),
                    )
                    TooltipIconButton(
                        onClick = { viewModel.editor.showLinkNoteDialog() },
                        icon = Icons.Default.AddLink,
                        tooltip = stringResource(R.string.link_note),
                    )
                    TooltipIconButton(
                        onClick = {
                            viewModel.editor.onEvent(
                                EditorEvent.InsertSyntax(
                                    "****",
                                    2,
                                ),
                            )
                        },
                        icon = Icons.Default.FormatBold,
                        tooltip = stringResource(R.string.bold),
                    )
                    TooltipIconButton(
                        onClick = {
                            viewModel.editor.onEvent(
                                EditorEvent.InsertSyntax(
                                    "**",
                                    1,
                                ),
                            )
                        },
                        icon = Icons.Default.FormatItalic,
                        tooltip = stringResource(R.string.italic),
                    )
                    TooltipIconButton(
                        onClick = {
                            viewModel.editor.onEvent(
                                EditorEvent.InsertSyntax(
                                    "``",
                                    1,
                                ),
                            )
                        },
                        icon = Icons.Default.Code,
                        tooltip = stringResource(R.string.inline_code),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(y = -ScreenOffset)
                    .zIndex(1f)
                    .onSizeChanged {
                        toolbarHeightDp = with(density) { it.height.toDp() + ScreenOffset * 3 }
                    },
            ) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above,
                    ),
                    tooltip = { PlainTooltip { Text(stringResource(R.string.edit_note)) } },
                    state = rememberTooltipState(),
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.editor.toggleViewingMode() },
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = stringResource(R.string.edit_note),
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                Column {
                    uiState.editorFrontMatter?.let { fm ->
                        FrontMatterProperties(
                            frontMatter = fm,
                            allTags = uiState.allProjectTags,
                            onUpdateKey = viewModel.editor::updateFmKey,
                            onUpdateValue = viewModel.editor::updateFmValue,
                            onAddProperty = viewModel.editor::addFmProperty,
                            onAddTag = viewModel.editor::addFmTag,
                            onRemoveTag = viewModel.editor::removeFmTag,
                            onRemoveProperty = viewModel.editor::removeFmProperty,
                        )
                    }
                    Box {
                        MarkdownEditorField(
                            state = viewModel.editor.state,
                            transformation = outputTransformation,
                            onTextLayout = onLayoutChange,
                            readOnly = isViewingMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = toolbarHeightDp,
                                )
                                .onFocusEvent {},
                        )

                        layoutState?.let { state ->
                            if (state.imageSpans.isNotEmpty() && uiState.project != null) {
                                state.imageSpans.forEach { span ->
                                    key(span.payload) {
                                        MarkdownImageOverlay(
                                            span = span,
                                            state = viewModel.editor.state,
                                            layoutResult = state.layout,
                                            project = uiState.project!!,
                                            density = density,
                                            editorWidth = editorWidth,
                                            imageAspectRatios = imageAspectRatios,
                                            onRatioMeasured = { path, ratio ->
                                                imageAspectRatios =
                                                    imageAspectRatios + (path to ratio)
                                            },
                                            isViewingMode = uiState.isViewingMode,
                                        )
                                    }
                                }
                            }

                            editorSpans
                                .filterIsInstance<SpanInfo.Link>()
                                .forEach { span ->
                                    key(span.payload) {
                                        MarkdownLinkOverlay(
                                            span = span,
                                            state = viewModel.editor.state,
                                            layoutResult = state.layout,
                                            viewModel = viewModel,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isLinkNoteDialogVisible) {
        Dialog(onDismissRequest = { viewModel.editor.dismissLinkNoteDialog() }) {
            Surface(
                shape = SearchBarDefaults.dockedShape,
            ) {
                Box(modifier = Modifier.padding(vertical = 8.dp)) {
                    val linkSearchResults =
                        viewModel.editor.linkSearchResultsPaged.collectAsLazyPagingItems()
                    NoteSearchBar(
                        searchState = viewModel.editor.linkSearchState,
                        searchResults = linkSearchResults,
                        onSearchEvent = viewModel.editor::onLinkSearchEvent,
                        paddingValues = PaddingValues(horizontal = 16.dp),
                        reverseLayout = uiState.settings?.reverseLayout
                            ?: Settings.EMPTY.reverseLayout,
                    ) { note ->
                        NoteDrawerItem(
                            name = note.name,
                            supportingText = if (!note.tags.isNullOrEmpty()) note.tags.joinToString(
                                ", ",
                            ) else null,
                            onClick = { viewModel.editor.insertNoteLink(note); focusManager.clearFocus() },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FrontMatterProperties(
    frontMatter: FrontMatter,
    allTags: List<String>,
    onUpdateKey: (String, String) -> Unit,
    onUpdateValue: (String, String) -> Unit,
    onAddProperty: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onRemoveProperty: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(top = 16.dp, bottom = 8.dp),
    ) {
        frontMatter.fields.forEach { (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var localKey by remember(key) { mutableStateOf(key) }
                val focusManager = LocalFocusManager.current

                BasicTextField(
                    value = localKey,
                    onValueChange = { localKey = it },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                if (localKey.isBlank() && key != CREATED_AT_FIELD && key != TAGS_FIELD) {
                                    onRemoveProperty(key)
                                } else if (localKey.isBlank()) {
                                    localKey = key
                                } else if (localKey != key) {
                                    onUpdateKey(key, localKey)
                                }
                            }
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    ),
                    readOnly = key == CREATED_AT_FIELD || key == TAGS_FIELD,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (localKey.isEmpty()) onRemoveProperty(key)
                            focusManager.clearFocus()
                        },
                    ),
                )

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(2f)) {
                    if (key == TAGS_FIELD) {
                        TagEditor(
                            tags = frontMatter.tags,
                            allTags = allTags,
                            onAddTag = onAddTag,
                            onRemoveTag = onRemoveTag,
                        )
                    } else if (value is FrontMatter.FrontMatterValue.Scalar || value is FrontMatter.FrontMatterValue.StringList) {
                        val realVal =
                            if (value is FrontMatter.FrontMatterValue.Scalar) value.value else ""
                        var localVal by remember(realVal) { mutableStateOf(realVal) }
                        BasicTextField(
                            value = localVal,
                            onValueChange = { localVal = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    if (!it.isFocused && localVal != realVal) onUpdateValue(
                                        key,
                                        localVal,
                                    )
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onAddProperty,
            modifier = Modifier.padding(horizontal = 2.dp),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.add_property))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagEditor(
    tags: List<String>,
    allTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val filtered by remember(text, allTags, tags) {
        derivedStateOf {
            allTags.filter {
                it.contains(
                    text,
                    ignoreCase = true,
                ) && !tags.contains(it)
            }
        }
    }
    val focusManager = LocalFocusManager.current

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        tags.forEach { tag ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(tag) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier
                            .size(InputChipDefaults.IconSize)
                            .clickable { onRemoveTag(tag) },
                    )
                },
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded && filtered.isNotEmpty(),
            onExpandedChange = { expanded = it },
        ) {
            BasicTextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    expanded = newText.isNotEmpty()
                },
                modifier = Modifier
                    .height(InputChipDefaults.Height)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = with(LocalDensity.current) { InputChipDefaults.Height.toSp() },
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) onAddTag(text.trim())
                        text = ""
                        expanded = false
                        focusManager.clearFocus()
                    },
                ),
            )

            ExposedDropdownMenu(
                expanded = expanded && filtered.isNotEmpty(),
                onDismissRequest = { expanded = false },
            ) {
                filtered.forEach { sugg ->
                    DropdownMenuItem(
                        text = { Text(sugg) },
                        onClick = {
                            onAddTag(sugg)
                            text = ""
                            expanded = false
                            focusManager.clearFocus()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownEditorField(
    state: TextFieldState,
    transformation: OutputTransformation,
    onTextLayout: (TextLayoutResult?) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    BasicTextField(
        state = state,
        readOnly = readOnly,
        textStyle = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            lineBreak = LineBreak.Paragraph,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
        ),
        outputTransformation = transformation,
        onTextLayout = { layoutProvider ->
            onTextLayout(layoutProvider())
        },
        modifier = modifier,
    )
}

@Composable
fun MarkdownImageOverlay(
    span: SpanInfo.Image,
    state: TextFieldState,
    layoutResult: TextLayoutResult,
    project: Project,
    density: Density,
    editorWidth: Int,
    imageAspectRatios: Map<String, Float>,
    onRatioMeasured: (String, Float) -> Unit,
    isViewingMode: Boolean,
) {
    val selection = state.selection
    val isSelected = selection.start <= span.range.end && selection.end >= span.range.start

    if (isSelected && !isViewingMode) return

    val path = span.payload
    val ratio = imageAspectRatios[path] ?: 1.777f
    val exactHeightPx = if (editorWidth > 0) editorWidth / ratio else 400f

    val layoutTextLength = layoutResult.layoutInput.text.length
    val offsetToUse = span.range.start.coerceIn(0, (layoutTextLength - 1).coerceAtLeast(0))

    val topPx = if (layoutTextLength > 0) {
        val lineIndex = layoutResult.getLineForOffset(offsetToUse)
        layoutResult.getLineTop(lineIndex)
    } else 0f

    val leftOffset = with(density) { 16.dp.roundToPx() }

    Box(
        modifier = Modifier
            .offset { IntOffset(leftOffset, topPx.toInt()) }
            .width(with(density) { editorWidth.toDp() })
            .height(with(density) { exactHeightPx.toDp() }),
    ) {
        AsyncMarkdownImage(
            path = path,
            project = project,
            onRatioMeasured = { newRatio -> onRatioMeasured(path, newRatio) },
        )
    }
}

@Composable
fun AsyncMarkdownImage(path: String, project: Project, onRatioMeasured: (Float) -> Unit) {
    var imageUri by remember(path, project) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(path, project) {
        // TODO: Change uri get and get it from a use case or something
        val rootUri = project.rootFileSystemPath.value.toUri()
        val relativePath = RelativePath(path)
        val treeId = DocumentsContract.getTreeDocumentId(rootUri)
        val childId = if (relativePath.value.isEmpty()) treeId else "$treeId/${relativePath.value}"
        imageUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, childId)
    }

    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val w = state.painter.intrinsicSize.width
                val h = state.painter.intrinsicSize.height
                if (w > 0 && h > 0) {
                    onRatioMeasured(w / h)
                }
            },
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MarkdownLinkOverlay(
    span: SpanInfo.Link,
    state: TextFieldState,
    layoutResult: TextLayoutResult,
    viewModel: AppViewModel,
) {
    LocalContext.current
    LocalUriHandler.current

    val selection = state.selection
    if (selection.start !in span.range.start..span.range.end) return
    if (selection.start !in 0..layoutResult.layoutInput.text.length) return

    val cursorRect = layoutResult.getCursorRect(selection.start)
    val name = span.label.ifEmpty { stringResource(R.string.editor_link_empty) }
    span.payload

    var surfaceHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    cursorRect.left.toInt(),
                    (cursorRect.top - surfaceHeight).toInt(),
                )
            },
    ) {
        Surface(
            shape = TooltipDefaults.richTooltipContainerShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp,
            modifier = Modifier
                .onSizeChanged { surfaceHeight = it.height },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = TooltipDefaults.richTooltipMaxWidth)
                    .padding(top = 12.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp),
                )
                TextButton(
                    onClick = {
                        viewModel.editor.openLink(
                            span = span,
                        )
                    },
                    modifier = Modifier
                        .padding(start = 4.dp),
                ) {
                    Text(stringResource(R.string.open))
                }
            }
        }
    }
}