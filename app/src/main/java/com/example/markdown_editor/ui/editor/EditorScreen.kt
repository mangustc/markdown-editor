package com.example.markdown_editor.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.domain.model.TokenType
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.activeNote) {
        viewModel.editorOnNoteOpened()
    }

    var imageAspectRatios by remember { mutableStateOf(mapOf<String, Float>()) }
    var editorWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val visualTransformation = remember(
        uiState.editorAnnotatedString,
        uiState.editorSpans,
        uiState.editorTextFieldValue.selection,
        imageAspectRatios,
        editorWidth,
        density
    ) {
        MarkdownVisualTransformation(
            annotated = uiState.editorAnnotatedString,
            spans = uiState.editorSpans,
            selection = uiState.editorTextFieldValue.selection,
            imageAspectRatios = imageAspectRatios,
            editorWidth = editorWidth,
            density = density
        )
    }

    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.editorOnEvent(EditorEvent.AttachPhoto(uri = uri))
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.editorOnEvent(EditorEvent.AttachFile(uri = uri))
        }
    }

    Scaffold(
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "****",
                            2
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "**",
                            1
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "``",
                            1
                        )
                    )
                }) {
                    Icon(Icons.Default.Code, contentDescription = "Inline code")
                }
                IconButton(onClick = { viewModel.editorOnSave() }) {
                    Icon(Icons.Default.Save, contentDescription = "Save file")
                }
                IconButton(onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Icon(Icons.Default.Image, contentDescription = "Attach photo")
                }
                IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imeNestedScroll()
                .verticalScroll(scrollState)
        ) {
            BasicTextField(
                value = uiState.editorTextFieldValue,
                onValueChange = { viewModel.editorOnContentChanged(it) },
                visualTransformation = visualTransformation,
                onTextLayout = {
                    textLayoutResult = it
                    if (editorWidth != it.size.width) {
                        editorWidth = it.size.width
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .padding(16.dp)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
            )

            textLayoutResult?.let { layoutResult ->
                val imageSpans = uiState.editorSpans.filter { it.type == TokenType.IMAGE }

                if (imageSpans.isNotEmpty() && uiState.project != null) {
                    imageSpans.forEachIndexed { index, span ->
                        val selection = uiState.editorTextFieldValue.selection
                        val isSelected = selection.start <= span.end && selection.end >= span.start

                        if (!isSelected) {
                            val rawText =
                                uiState.editorTextFieldValue.text.substring(span.start, span.end)
                            val path = extractImagePath(rawText)

                            val ratio = imageAspectRatios[path] ?: 1.777f
                            val exactHeightPx = if (editorWidth > 0) editorWidth / ratio else 400f

                            val layoutTextLength = layoutResult.layoutInput.text.length
                            val offsetToUse =
                                span.start.coerceIn(0, (layoutTextLength - 1).coerceAtLeast(0))

                            var topPx = 0f
                            if (layoutTextLength > 0) {
                                val lineIndex = layoutResult.getLineForOffset(offsetToUse)
                                topPx = layoutResult.getLineTop(lineIndex)
                            }

                            if (path.isNotEmpty()) {
                                val topOffset = topPx + with(density) { 16.dp.toPx() }
                                val leftOffset = with(density) { 16.dp.roundToPx() }

                                key(path, ratio) {
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(leftOffset, topOffset.toInt()) }
                                            .width(with(density) { editorWidth.toDp() })
                                            .height(with(density) { exactHeightPx.toDp() })
                                    ) {
                                        AsyncMarkdownImage(
                                            path = path,
                                            project = uiState.project!!,
                                            onRatioMeasured = { newRatio ->
                                                if (imageAspectRatios[path] != newRatio) {
                                                    imageAspectRatios =
                                                        imageAspectRatios + (path to newRatio)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun extractImagePath(markdown: String): String {
    return markdown
        .substringAfter("](")
        .dropLast(1)
        .substringBefore(" \"")
        .trim()
        .removeSurrounding("<", ">")
}

@Composable
fun AsyncMarkdownImage(path: String, project: Project, onRatioMeasured: (Float) -> Unit) {
    val context = LocalContext.current
    var imageUri by remember(path, project) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(path, project) {
        withContext(Dispatchers.IO) {
            try {
                if (path.startsWith("assets/")) {
                    val fileName = path.removePrefix("assets/")
                    val root = DocumentFile.fromTreeUri(context, project.assetsUri)
                    imageUri = root?.findFile(fileName)?.uri
                } else {
                    imageUri = path.toUri()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}