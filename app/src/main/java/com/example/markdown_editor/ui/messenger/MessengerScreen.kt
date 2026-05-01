package com.example.markdown_editor.ui.messenger

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.util.LinkPreviewFetcher
import com.example.markdown_editor.domain.parser.MarkdownParser
import com.example.markdown_editor.ui.components.MenuPopup
import com.example.markdown_editor.ui.components.MenuPopupGroup
import com.example.markdown_editor.ui.components.MenuPopupItem
import com.example.markdown_editor.ui.util.scrollbar
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val URL_PATTERN = Regex("""https?://[^\s<>"')]+""")

private data class ParsedNoteBody(
    val text: String,
    val attachments: List<Attachment>,
)

private fun extractLinkPath(matched: String): String =
    matched
        .substringAfter("](")
        .dropLast(1)
        .substringBefore(" \"")
        .trim()
        .removeSurrounding("<", ">")

private fun extractLinkLabel(matched: String): String =
    matched.substringAfter("[").substringBefore("]")

private fun parseNoteBody(body: String, project: Project): ParsedNoteBody {
    val photoAttachments = MarkdownParser.IMAGE_REGEX.findAll(body)
        .map { match ->
            val path = extractLinkPath(match.value)
            val uri = project.getFileUri(path)
            Attachment.Photo(uri = uri, path = path)
        }
        .toList()

    val fileAttachments = MarkdownParser.FILE_REGEX.findAll(body)
        .map { match ->
            val label = extractLinkLabel(match.value)
            val path = extractLinkPath(match.value)
            val uri = project.getFileUri(path)
            Attachment.AttachedFile(uri = uri, displayName = label, path = path)
        }
        .toList()

    var text = body

    MarkdownParser.IMAGE_REGEX.findAll(body)
        .map { it.value }
        .forEach { text = text.replace(it, "") }

    MarkdownParser.FILE_REGEX.findAll(body)
        .map { it.value }
        .forEach { text = text.replace(it, "") }

    return ParsedNoteBody(
        text = text.trim(),
        attachments = photoAttachments + fileAttachments,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val attachments = remember { mutableStateListOf<Attachment>() }
    var photoPagerState by remember { mutableStateOf<Pair<Int, List<Uri>>?>(null) }
    var carouselExpanded by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        uris.forEach { uri ->
            attachments.add(Attachment.PendingPhoto(uri))
        }
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        uris.forEach { uri ->
            attachments.add(Attachment.PendingAttachedFile(uri, null))
        }
    }

    LaunchedEffect(uiState.project) {
        uiState.project?.let { viewModel.messenger.onMessengerOpened(it) }
    }

    LaunchedEffect(Unit) {
        val pending = viewModel.consumePendingIntentAttachments()
        if (pending.isNotEmpty()) carouselExpanded = true
        attachments.addAll(pending)
    }
    val pagedNotes = viewModel.messenger.notesPaged.collectAsLazyPagingItems()

    val listState = rememberLazyListState()
    LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    var inputBarHeightDp by remember { mutableStateOf(0.dp) }

    if (!uiState.messengerIsLoading && uiState.project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Open a project folder to see notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            MessengerInputBar(
                value = uiState.messengerNewNoteText,
                onValueChange = { viewModel.messenger.onNewNoteTextChanged(it) },
                attachments = attachments,
                isEditing = uiState.messengerEditingNote != null,
                onCancelEdit = {
                    viewModel.messenger.cancelEditNote()
                    attachments.clear()
                    carouselExpanded = false
                },
                onAddPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onAddFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                onPhotoClick = { clickedUri ->
                    val photoUris = attachments.mapNotNull {
                        when (it) {
                            is Attachment.Photo -> it.uri
                            is Attachment.PendingPhoto -> it.uri
                            else -> null
                        }
                    }
                    val index = photoUris.indexOf(clickedUri)
                    photoPagerState = index to photoUris
                },
                onFileClick = { uri ->
                    try {
                        val mime =
                            context.contentResolver.getType(uri)
                                ?: "*/*"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, mime)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(intent, null),
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context, "No app found to open this file",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                onRemoveAttachment = { index -> attachments.removeAt(index) },
                onSend = {
                    val snapshot = attachments.toList()
                    attachments.clear()
                    if (uiState.messengerEditingNote != null) {
                        viewModel.messenger.onSaveEditedNote(
                            attachments = snapshot,
                            afterUpdate = {},
                        )
                    } else {
                        viewModel.messenger.onSendNote(
                            attachments = snapshot,
                            afterUpdate = {
                                scope.launch {
                                    if (pagedNotes.itemCount != 0) listState.animateScrollToItem(
                                        0,
                                    )
                                }
                            },
                        )
                    }
                },
                project = uiState.project,
                carouselExpanded = carouselExpanded,
                onCarouselExpandClick = { carouselExpanded = !carouselExpanded },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1f)
                    .onSizeChanged { inputBarHeightDp = with(density) { it.height.toDp() } }
                    .fillMaxWidth(),
            )
            when {
                uiState.messengerIsLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator()
                    }
                }

                pagedNotes.itemCount == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No quick notes yet.\nType something below to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scrollbar(listState),
                    ) {
                        item(key = "input_spacer") {
                            Spacer(modifier = Modifier.height(inputBarHeightDp))
                        }
                        items(
                            count = pagedNotes.itemCount,
                            key = pagedNotes.itemKey { it.uri.toString() },
                        ) { index ->
                            val note = pagedNotes[index]
                            if (note != null) {
                                MessageBubble(
                                    note = note,
                                    project = uiState.project!!,
                                    linkPreviews = uiState.messengerLinkPreviews,
                                    onEnsurePreview = { viewModel.messenger.ensureLinkPreview(it) },
                                    onNoteSelected = { viewModel.navigation.onNoteSelected(it) },
                                    onDeleteNote = { viewModel.navigation.onDeleteNote(it) },
                                    onEditNote = { n, text, attach ->
                                        viewModel.messenger.startEditNote(n, text)
                                        attachments.clear()
                                        attachments.addAll(attach)
                                        if (attachments.isNotEmpty()) carouselExpanded = true
                                    },
                                    onPhotoClick = { idx, uris -> photoPagerState = idx to uris },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    photoPagerState?.let { (index, uris) ->
        FullScreenPhotoCarouselDialog(
            initialIndex = index,
            uris = uris,
            onDismiss = { photoPagerState = null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MessengerInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    attachments: List<Attachment>,
    isEditing: Boolean,
    onCancelEdit: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddFile: () -> Unit,
    onPhotoClick: (Uri) -> Unit,
    onFileClick: (Uri) -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onSend: () -> Unit,
    project: Project?,
    carouselExpanded: Boolean,
    onCarouselExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(
                color = if (carouselExpanded || isEditing) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent,
            )
            .padding(8.dp),
    ) {
        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Editing note",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onCancelEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel editing",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = carouselExpanded,
            enter = expandVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            AttachmentCarouselStrip(
                attachments = attachments,
                onAddPhoto = onAddPhoto,
                onAddFile = onAddFile,
                onRemove = onRemoveAttachment,
                onPhotoClick = onPhotoClick,
                onFileClick = onFileClick,
                isViewing = false,
                project = project,
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(if (isEditing) "Edit note…" else "New quick note…") },
            shape = MaterialTheme.shapes.extraLargeIncreased,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            leadingIcon = {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Attach content") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onCarouselExpandClick,
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach content")
                    }
                }
            },
            trailingIcon = {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(if (isEditing) "Save changes" else "Create note") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onSend,
                        colors = IconButtonDefaults.iconButtonColors(
                            disabledContentColor = Color.Unspecified,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        enabled = value.isNotBlank() || attachments.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isEditing) "Save changes" else "Create note",
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun AttachmentCarouselStrip(
    attachments: List<Attachment>,
    onAddPhoto: (() -> Unit)? = null,
    onAddFile: (() -> Unit)? = null,
    onRemove: ((Int) -> Unit)? = null,
    onPhotoClick: (Uri) -> Unit,
    onFileClick: (Uri) -> Unit,
    project: Project?,
    isViewing: Boolean,
) {
    val state = rememberCarouselState { if (isViewing) attachments.size else attachments.size + 2 }
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = 80.dp,
        itemSpacing = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            if (!isViewing && page == 0) {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Attach photos") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onAddPhoto ?: {},
                        shape = IconButtonDefaults.smallSquareShape,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Image",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Add photos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else if (!isViewing && page == 1) {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Attach files") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onAddFile ?: {},
                        shape = IconButtonDefaults.smallSquareShape,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "File",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Add files",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                when (val attachment = attachments[if (isViewing) page else page - 2]) {
                    is Attachment.Photo -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp,
                            onClick = { onPhotoClick(attachment.uri) },
                        ) {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    is Attachment.PendingPhoto -> {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            onClick = { attachment.uri.let(onPhotoClick) },
                            tonalElevation = 2.dp,
                        ) {
                            AsyncImage(
                                model = attachment.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    is Attachment.AttachedFile -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp,
                            onClick = { onFileClick(attachment.uri) },
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = attachment.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    is Attachment.PendingAttachedFile -> {
                        val context = LocalContext.current
                        val displayName = remember(attachment.uri) {
                            DocumentFile.fromSingleUri(context, attachment.uri)?.name ?: "File"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium,
                            onClick = { onFileClick(attachment.uri) },
                            modifier = Modifier
                                .fillMaxSize(),
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                if (onRemove != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { onRemove(page - 2) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    note: Note,
    project: Project,
    linkPreviews: Map<String, LinkPreview?>,
    onEnsurePreview: (String) -> Unit,
    onNoteSelected: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onEditNote: (Note, String, List<Attachment>) -> Unit,
    onPhotoClick: (Int, List<Uri>) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current

    val urls = remember(note.body) { LinkPreviewFetcher.extractAllUrls(note.body ?: "") }
    LaunchedEffect(urls) { urls.forEach { onEnsurePreview(it) } }

    val previews = remember(urls, linkPreviews) { urls.mapNotNull { linkPreviews[it] } }
    val parsedBody = remember(note.body) { parseNoteBody(note.body ?: "", project) }
    val bodyText = parsedBody.text.ifBlank { "" }
    val timeString = remember(note.createdAt, note.lastModified) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(
            Date(
                note.createdAt ?: note.lastModified,
            ),
        )
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var touchX by remember { mutableStateOf(0.dp) }
    var touchY by remember { mutableStateOf(0.dp) }

    val urlColor = MaterialTheme.colorScheme.primary
    val annotatedBody = remember(bodyText) {
        buildAnnotatedString {
            var lastIndex = 0
            URL_PATTERN.findAll(bodyText).forEach { match ->
                append(bodyText.substring(lastIndex, match.range.first))
                pushStringAnnotation(tag = "URL", annotation = match.value)
                withStyle(SpanStyle(color = urlColor, textDecoration = TextDecoration.Underline)) {
                    append(match.value)
                }
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex < bodyText.length) append(bodyText.substring(lastIndex))
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitFirstDown(requireUnconsumed = false)
                    touchX = with(density) { event.position.x.toDp() }
                    touchY = with(density) { event.position.y.toDp() }
                }
            }
            .clickable(onClick = { menuExpanded = true })
            .padding(horizontal = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        ),
                    ),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (parsedBody.attachments.isNotEmpty()) {
                        AttachmentCarouselStrip(
                            attachments = parsedBody.attachments,
                            project = project,
                            onPhotoClick = { clickedUri ->
                                val photoUris = parsedBody.attachments.mapNotNull {
                                    when (it) {
                                        is Attachment.Photo -> it.uri
                                        is Attachment.PendingPhoto -> it.uri
                                        else -> null
                                    }
                                }
                                onPhotoClick(photoUris.indexOf(clickedUri), photoUris)
                            },
                            onFileClick = { uri ->
                                try {
                                    val mime = context.contentResolver.getType(uri) ?: "*/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "No app found to open this file",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            isViewing = true,
                        )
                        if (bodyText.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (bodyText.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = annotatedBody,
                                style = TextStyle(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                                ),
                                onTextLayout = { layoutResult.value = it },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            layoutResult.value?.let { result ->
                                                val position = result.getOffsetForPosition(offset)
                                                annotatedBody.getStringAnnotations(
                                                    "URL",
                                                    position,
                                                    position,
                                                )
                                                    .firstOrNull()
                                                    ?.let { uriHandler.openUri(it.item) }
                                                    ?: run { menuExpanded = true }
                                            }
                                        },
                                        onLongPress = { offset ->
                                            layoutResult.value?.let { result ->
                                                val position = result.getOffsetForPosition(offset)
                                                annotatedBody.getStringAnnotations(
                                                    "URL",
                                                    position,
                                                    position,
                                                )
                                                    .firstOrNull()?.let { link ->
                                                        scope.launch {
                                                            clipboard.setClipEntry(
                                                                ClipEntry(
                                                                    ClipData.newPlainText(
                                                                        "URL",
                                                                        link.item,
                                                                    ),
                                                                ),
                                                            )
                                                            Toast.makeText(
                                                                context,
                                                                "Link copied",
                                                                Toast.LENGTH_SHORT,
                                                            ).show()
                                                            focusManager.clearFocus()
                                                        }
                                                    }
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    }

                    if (previews.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkPreviewCarousel(previews = previews)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = note.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.offset { IntOffset(touchX.roundToPx(), touchY.roundToPx()) }) {
            MenuPopup(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { gs ->
                MenuPopupGroup(index = 0, count = 1, label = "Actions", interactionSource = gs) {
                    MenuPopupItem(
                        text = "Open", index = 0, count = 4,
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { menuExpanded = false; onNoteSelected(note) },
                    )
                    MenuPopupItem(
                        text = "Copy", index = 1, count = 4,
                        icon = Icons.Outlined.ContentCopy,
                        onClick = {
                            menuExpanded = false
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText(
                                            "Note text",
                                            bodyText,
                                        ),
                                    ),
                                )
                            }
                        },
                    )
                    MenuPopupItem(
                        text = "Edit", index = 2, count = 4,
                        icon = Icons.Outlined.Edit,
                        onClick = {
                            menuExpanded = false; onEditNote(
                            note,
                            parsedBody.text,
                            parsedBody.attachments,
                        )
                        },
                    )
                    MenuPopupItem(
                        text = "Delete", index = 3, count = 4,
                        supportingText = "Cannot be undone",
                        icon = Icons.Outlined.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        onClick = { menuExpanded = false; onDeleteNote(note) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenPhotoCarouselDialog(
    initialIndex: Int,
    uris: List<Uri>,
    onDismiss: () -> Unit,
) {
    val state = rememberCarouselState(initialItem = initialIndex) { uris.size }
    var showTopPanel by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalUncontainedCarousel(
                state = state,
                itemWidth = Dp.Infinity,
                itemSpacing = 0.dp,
                flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state),
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableImage(
                    uri = uris[page],
                    onTap = { showTopPanel = !showTopPanel },
                )
            }

            AnimatedVisibility(
                visible = showTopPanel,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                        tooltip = { PlainTooltip { Text("Go back") } },
                        state = rememberTooltipState(),
                        modifier = Modifier
                            .align(Alignment.CenterStart),
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    }

                    Text(
                        text = "${state.currentItem + 1} of ${uris.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(uri: Uri, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    var totalPan = Offset.Zero

                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val pointers = event.changes.size

                        totalPan += pan

                        if (!moved && (zoom != 1f || totalPan.getDistance() > slop || pointers > 1)) {
                            moved = true
                        }

                        if (moved) {
                            scale = (scale * zoom).coerceIn(1f, 4f)

                            if (scale > 1f) {
                                val maxX = (containerSize.width * (scale - 1)) / 2f
                                val maxY = (containerSize.height * (scale - 1)) / 2f

                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkPreviewCarousel(previews: List<LinkPreview>) {
    val state = rememberCarouselState { previews.size }
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalUncontainedCarousel(
            state = state,
            itemWidth = Dp.Infinity,
            itemSpacing = 8.dp,
            flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) { page ->
            val preview = previews[page]
            val uriHandler = LocalUriHandler.current

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { uriHandler.openUri(preview.url) },
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column {
                    if (!preview.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = preview.imageUrl,
                            contentDescription = preview.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }

                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = remember(preview.url) {
                                runCatching { URL(preview.url).host.removePrefix("www.") }
                                    .getOrDefault(preview.url)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!preview.title.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preview.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!preview.description.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preview.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        if (previews.size > 1) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text(
                    text = "${state.currentItem + 1}/${previews.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}