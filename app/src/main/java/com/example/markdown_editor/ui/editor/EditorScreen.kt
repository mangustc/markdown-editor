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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import com.example.markdown_editor.ui.editor.EditorEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.example.markdown_editor.domain.model.TokenType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Load note content whenever the active URI changes
    LaunchedEffect(uiState.activeNote) {
        viewModel.editorOnNoteOpened()
    }
    val visualTransformation = remember(uiState.editorAnnotatedString, uiState.editorSpans) {
        MarkdownVisualTransformation(uiState.editorAnnotatedString, uiState.editorSpans)
    }

    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = uri.let { u ->
                // Determine MIME from the URI; fallback to "image/*"
                "image/*"
            }
            viewModel.editorOnEvent(EditorEvent.AttachPhoto(uri = uri, mimeType = mime))
        }
    }

    // ── File picker (any file via SAF) ──────────────────────────────────────
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.editorOnEvent(
                EditorEvent.AttachFile(
                    uri         = uri,
                    mimeType    = null,   // ViewModel will resolve via ContentResolver
                    displayName = null
                )
            )
        }
    }

    Scaffold(
        // No TopAppBar here — AppScaffold owns that
        bottomBar = {
            BottomAppBar(
                modifier = Modifier.imePadding(), // always sits above the keyboard
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "****",
                            cursorOffset = 2
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "**",
                            cursorOffset = 1
                        )
                    )
                }) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = {
                    viewModel.editorOnEvent(
                        EditorEvent.InsertSyntax(
                            "``",
                            cursorOffset = 1
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
                IconButton(onClick = {
                    filePickerLauncher.launch(arrayOf("*/*"))
                }) {
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
                onTextLayout = { textLayoutResult = it },
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
                if (imageSpans.isNotEmpty()) {
                    val transformedText = visualTransformation.filter(uiState.editorAnnotatedString)
                    val offsetMapping = transformedText.offsetMapping
                    imageSpans.forEach { span ->
                        val rawText = uiState.editorTextFieldValue.text.substring(span.start, span.end)
                        val path = extractImagePath(rawText)

                        val transformedOffset = offsetMapping.originalToTransformed(span.end)
                        val spacerStartOffset = transformedOffset - MarkdownVisualTransformation.SPACER.length

                        if (spacerStartOffset >= 0 && spacerStartOffset <= layoutResult.layoutInput.text.length) {
                            val line = layoutResult.getLineForOffset(spacerStartOffset)
                            val yCoordinate = layoutResult.getLineBottom(line)

                            if (path.isNotEmpty() && uiState.project != null) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                        .offset { IntOffset(0, yCoordinate.toInt()) }
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .padding(vertical = 8.dp)
                                ) {
                                    AsyncMarkdownImage(path = path, project = uiState.project!!)
                                }
                            }
                        }
                    }
                }
            }        }
    }
}


private fun extractImagePath(markdown: String): String {
    return markdown
        .substringAfter("](")
        .dropLast(1)
        .substringBefore(" \"")
        .trim()
        .removeSurrounding("<", ">")
}
@Composable
fun AsyncMarkdownImage(path: String, project: Project) {
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
            contentScale = ContentScale.Fit
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