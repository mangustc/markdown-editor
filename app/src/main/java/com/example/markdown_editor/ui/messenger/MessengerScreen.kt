package com.example.markdown_editor.ui.messenger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.util.LinkPreviewFetcher
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val URL_PATTERN = Regex("""https?://[^\s<>"'\)]+""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.project) {
        uiState.project?.let { viewModel.messengerOnMessengerOpened(it) }
    }

    val sortedNotes = remember(uiState.messengerNotesList) {
        uiState.messengerNotesList.sortedByDescending { it.createdAt ?: it.lastModified }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(sortedNotes.size) {
        if (sortedNotes.isNotEmpty()) listState.animateScrollToItem(0)
    }

    if (uiState.project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Open a project folder to see notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                when {
                    uiState.messengerIsLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    sortedNotes.isEmpty() -> {
                        Text(
                            text = "No quick notes yet.\nType something below to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            reverseLayout = true,
                        ) {
                            items(sortedNotes, key = { it.uri.toString() }) { note ->
                                val urls = remember(note.body) {
                                    LinkPreviewFetcher.extractAllUrls(note.body ?: "")
                                }
                                // Trigger fetch for every URL in this note
                                LaunchedEffect(urls) {
                                    urls.forEach { viewModel.messengerEnsureLinkPreview(it) }
                                }
                                // Collect only the previews that have already loaded
                                val previews = remember(urls, uiState.messengerLinkPreviews) {
                                    urls.mapNotNull { uiState.messengerLinkPreviews[it] }
                                }

                                MessageBubble(
                                    note = note,
                                    previews = previews,
                                    onClick = { viewModel.onNoteSelected(note) }
                                )
                            }
                        }
                    }
                }
            }
            MessengerInputBar(
                value = uiState.messengerNewNoteText,
                onValueChange = { viewModel.messengerOnNewNoteTextChanged(it) },
                onSend = { viewModel.messengerOnSendNote() }
            )
        }
    }
}

@Composable
private fun MessageBubble(
    note: Note,
    previews: List<LinkPreview>,
    onClick: () -> Unit,
) {
    val timeString = remember(note.createdAt, note.lastModified) {
        val millis = note.createdAt ?: note.lastModified
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))
    }

    val uriHandler = LocalUriHandler.current
    val bodyText = note.body?.trim()?.ifBlank { note.name } ?: note.name

    val annotatedBody = remember(bodyText) {
        buildAnnotatedString {
            var lastIndex = 0
            URL_PATTERN.findAll(bodyText).forEach { match ->
                append(bodyText.substring(lastIndex, match.range.first))
                pushStringAnnotation(tag = "URL", annotation = match.value)
                withStyle(
                    SpanStyle(
                        color = Color(0xFF1A73E8),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match.value)
                }
                pop()
                lastIndex = match.range.last + 1
            }
            if (lastIndex < bodyText.length) append(bodyText.substring(lastIndex))
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier
                .widthIn(min = 80.dp, max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                ClickableText(
                    text = annotatedBody,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        annotatedBody.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    }
                )

                if (previews.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinkPreviewCarousel(previews = previews)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

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
                .wrapContentHeight()
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
                    .padding(12.dp)
            ) {
                Text(
                    text = "${state.currentItem + 1}/${previews.size}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MessengerInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("New quick note…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 6,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                )
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send note")
            }
        }
    }
}