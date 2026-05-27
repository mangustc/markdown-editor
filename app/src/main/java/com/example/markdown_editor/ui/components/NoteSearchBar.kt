package com.example.markdown_editor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.markdown_editor.R
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.ui.util.scrollbar
import com.example.markdown_editor.ui.viewmodel.events.SearchEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteSearchBar(
    searchState: TextFieldState,
    searchResults: LazyPagingItems<Note>,
    onSearchEvent: (SearchEvent) -> Unit,
    paddingValues: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    itemContent: @Composable (note: Note) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var isUserClick by remember { mutableStateOf(false) }

    val searchResultsTextComponent = @Composable {
        Text(
            text = if (searchResults.itemCount == 0) stringResource(R.string.no_matches) else "Found: ${searchResults.itemCount}",
            modifier = Modifier.padding(
                paddingValues = paddingValues + if (reverseLayout) PaddingValues(top = 8.dp) else PaddingValues(
                    bottom = 8.dp,
                ),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    val searchResultsComponent = @Composable {
        val searchListState = rememberLazyListState()
        LazyColumn(
            state = searchListState,
            reverseLayout = reverseLayout,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .scrollbar(searchListState),
        ) {
            items(
                count = searchResults.itemCount,
                key = searchResults.itemKey { it.projectFile.relativePath.value },
            ) { index ->
                val note = searchResults[index]
                if (note != null) {
                    itemContent(note)
                }
            }
        }
    }

    val suggestionsComponent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = {
                    onSearchEvent(SearchEvent.AppendTag)
                    isUserClick = true
                    focusRequester.requestFocus()
                },
                label = { Text("tag:") },
                icon = {
                    Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(SuggestionChipDefaults.IconSize),
                    )
                },
            )
            SuggestionChip(
                onClick = {
                    onSearchEvent(SearchEvent.AppendName)
                    isUserClick = true
                    focusRequester.requestFocus()
                },
                label = { Text("name:") },
                icon = {
                    Icon(
                        Icons.Default.Abc,
                        contentDescription = null,
                        modifier = Modifier.size(SuggestionChipDefaults.IconSize),
                    )
                },
            )
            SuggestionChip(
                onClick = { onSearchEvent(SearchEvent.ToggleNegation) },
                label = { Text(stringResource(R.string.reverse)) },
                icon = {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(SuggestionChipDefaults.IconSize),
                    )
                },
            )
        }
    }

    val inputFieldComponent = @Composable {
        OutlinedTextField(
            state = searchState,
            placeholder = { Text(stringResource(R.string.search_notes)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchState.text.isNotEmpty()) {
                    TooltipIconButton(
                        onClick = { onSearchEvent(SearchEvent.Clear) },
                        icon = Icons.Default.Close,
                        tooltip = stringResource(R.string.clear_search),
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .focusRequester(focusRequester)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown(requireUnconsumed = false)
                            isUserClick = true
                        }
                    }
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && !isUserClick) {
                        focusManager.clearFocus()
                    }
                    if (!focusState.isFocused) {
                        isUserClick = false
                    }
                },
        )
    }

    Column {
        if (reverseLayout) {
            searchResultsComponent()
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            searchResultsTextComponent()
            suggestionsComponent()
            HorizontalDivider(modifier = Modifier.padding(paddingValues))
            inputFieldComponent()
        } else {
            inputFieldComponent()
            HorizontalDivider(modifier = Modifier.padding(paddingValues))
            suggestionsComponent()
            searchResultsTextComponent()
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            searchResultsComponent()
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun NoteDrawerItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    onRename: () -> Unit,
    supportingText: String? = null,
    isPinned: Boolean = false,
    containerColor: Color = Color.Transparent,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    remember { MutableInteractionSource() }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        ListItem(
            onClick = onClick,
            onLongClick = { menuExpanded = true },
            content = {
                Text(
                    text = buildAnnotatedString {
                        if (isPinned) {
                            appendInlineContent("inlinePinned", "[icon]")
                        }
                        append((if (isPinned) " " else "") + name)
                    },
                    inlineContent = if (isPinned) mapOf(
                        Pair(
                            "inlinePinned",
                            InlineTextContent(
                                Placeholder(
                                    width = 1.em,
                                    height = 1.em,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                ),
                            ) {
                                Icon(
                                    Icons.Filled.PushPin,
                                    "pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                        ),
                    ) else mapOf(),
                    style = LocalTextStyle.current.copy(
                        lineBreak = LineBreak.Paragraph,
                    ),
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else Color.Unspecified,
                )
            },
            supportingContent = {
                if (supportingText != null) {
                    Text(supportingText)
                }
            },
            colors = ListItemDefaults.colors(containerColor = containerColor),
            trailingContent = {
                TooltipIconButton(
                    onClick = { menuExpanded = true },
                    icon = Icons.Default.MoreVert,
                    tooltip = stringResource(R.string.note_actions),
                )
                MenuPopup(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) { groupInteractionSource ->
                    MenuPopupGroup(
                        index = 0,
                        count = 2,
                        label = stringResource(R.string.actions),
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuPopupItem(
                            text = stringResource(R.string.open),
                            index = 0, count = 4,
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = { menuExpanded = false; onOpen() },
                        )

                        MenuPopupItem(
                            text = stringResource(R.string.rename),
                            index = 1, count = 4,
                            icon = Icons.Outlined.DriveFileRenameOutline,
                            onClick = { menuExpanded = false; onRename() },
                        )

                        MenuPopupItem(
                            text = if (isPinned) stringResource(R.string.unpin) else stringResource(
                                R.string.pin,
                            ),
                            index = 1, count = 4,
                            icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            onClick = { menuExpanded = false; onPin() },
                        )

                        MenuPopupItem(
                            text = stringResource(R.string.delete),
                            index = 2, count = 4,
                            supportingText = stringResource(R.string.cannot_be_undone),
                            icon = Icons.Outlined.Delete,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }

                    Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                    MenuPopupGroup(
                        index = 1,
                        count = 2,
                        label = stringResource(R.string.more),
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuPopupItem(
                            text = stringResource(R.string.show_details),
                            index = 3, count = 4,
                            icon = Icons.Outlined.Info,
                            onClick = { menuExpanded = false; onShowInfo() },
                        )
                    }
                }
            },
        )

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteDrawerItem(
    name: String,
    onClick: () -> Unit,
    supportingText: String? = null,
    containerColor: Color = Color.Unspecified,
) {
    ListItem(
        content = {
            Text(
                text = name,
                style = LocalTextStyle.current.copy(
                    lineBreak = LineBreak.Paragraph,
                ),
            )
        },
        supportingContent = {
            if (supportingText != null) {
                Text(supportingText)
            }
        },
        colors = ListItemDefaults.colors(containerColor = containerColor),
        onClick = onClick,
    )
}

