package com.example.markdown_editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.markdown_editor.R
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.editor.EditorEvent
import com.example.markdown_editor.domain.markdown.MarkdownParser
import com.example.markdown_editor.domain.model.SpanInfo
import com.example.markdown_editor.domain.model.TokenType
import com.example.markdown_editor.domain.viewmodel.AppViewModel
import com.example.markdown_editor.ui.components.TooltipIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EditorLayoutState(
    val layout: TextLayoutResult,
    val imageSpans: List<SpanInfo>,
)

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class,
)
@Composable
fun EditorScreen(
    viewModel: AppViewModel,
    noteUriString: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(noteUriString) {
        viewModel.editor.editorOnNoteOpened(noteUriString)
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var toolbarHeightDp by remember { mutableStateOf(0.dp) }

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
                val images = editorSpans.filter { it.type == TokenType.IMAGE }
                layoutState = EditorLayoutState(layoutResult, images)
                if (editorWidth != layoutResult.size.width) {
                    editorWidth = layoutResult.size.width
                }
            }
        }
    }
    val outputTransformation = remember {
        MarkdownOutputTransformation(
            state = viewModel.editor.state,
            density = density,
            widthProvider = { editorWidth },
            spansProvider = { editorSpans },
            ratiosProvider = { imageAspectRatios },
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.editor.editorOnEvent(EditorEvent.AttachPhoto(uri = uri))
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.editor.editorOnEvent(EditorEvent.AttachFile(uri = uri))
        }
    }

    Box(
        modifier = Modifier
            .imePadding(),
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            expandedShadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -ScreenOffset)
                .zIndex(1f)
                .onSizeChanged {
                    toolbarHeightDp = with(density) { it.height.toDp() + ScreenOffset * 3 }
                },
        ) {
            TooltipIconButton(
                onClick = { viewModel.editor.editorOnEvent(EditorEvent.Undo) },
                icon = Icons.AutoMirrored.Filled.Undo,
                tooltip = stringResource(R.string.undo),
                enabled = viewModel.editor.state.undoState.canUndo,
            )
            TooltipIconButton(
                onClick = { viewModel.editor.editorOnEvent(EditorEvent.Redo) },
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
                onClick = {
                    viewModel.editor.editorOnEvent(
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
                    viewModel.editor.editorOnEvent(
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
                    viewModel.editor.editorOnEvent(
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
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .imeNestedScroll()
                    .verticalScroll(scrollState),
            ) {
                MarkdownEditorField(
                    state = viewModel.editor.state,
                    transformation = outputTransformation,
                    onTextLayout = onLayoutChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .padding(start = 16.dp, end = 16.dp, bottom = toolbarHeightDp)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    delay(300)
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                )

                layoutState?.let { state ->
                    if (state.imageSpans.isNotEmpty() && uiState.project != null) {
                        state.imageSpans.forEach { span ->
                            key(span.payload ?: span.start) {
                                MarkdownImageOverlay(
                                    span = span,
                                    state = viewModel.editor.state,
                                    layoutResult = state.layout,
                                    project = uiState.project!!,
                                    density = density,
                                    editorWidth = editorWidth,
                                    imageAspectRatios = imageAspectRatios,
                                    onRatioMeasured = { path, ratio ->
                                        imageAspectRatios = imageAspectRatios + (path to ratio)
                                    },
                                )
                            }
                        }
                    }
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
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    BasicTextField(
        state = state,
        textStyle = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            lineBreak = LineBreak.Paragraph,
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
    span: SpanInfo,
    state: TextFieldState,
    layoutResult: TextLayoutResult,
    project: Project,
    density: Density,
    editorWidth: Int,
    imageAspectRatios: Map<String, Float>,
    onRatioMeasured: (String, Float) -> Unit,
) {
    val selection = state.selection
    val isSelected = selection.start <= span.end && selection.end >= span.start

    if (isSelected) return

    val path = span.payload ?: return
    val ratio = imageAspectRatios[path] ?: 1.777f
    val exactHeightPx = if (editorWidth > 0) editorWidth / ratio else 400f

    val layoutTextLength = layoutResult.layoutInput.text.length
    val offsetToUse = span.start.coerceIn(0, (layoutTextLength - 1).coerceAtLeast(0))

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
        imageUri = project.getFileUri(path)
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