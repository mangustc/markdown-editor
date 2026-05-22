package com.example.markdown_editor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.domain.navigation.SearchEvent
import com.example.markdown_editor.ui.util.scrollbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSearchBar(
    searchState: TextFieldState,
    searchResults: LazyPagingItems<Note>,
    onSearchEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (Note) -> Unit,
) {
    DockedSearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                state = searchState,
                onSearch = {},
                expanded = true,
                onExpandedChange = {},
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
            )
        },
        expanded = true,
        onExpandedChange = {},
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = { onSearchEvent(SearchEvent.AppendTag) },
                label = { Text("tag:") },
                icon = {
                    Icon(
                        Icons.Default.Tag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            SuggestionChip(
                onClick = { onSearchEvent(SearchEvent.AppendName) },
                label = { Text("name:") },
                icon = {
                    Icon(
                        Icons.Default.Abc,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
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
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        if (searchResults.itemCount == 0) {
            Text(
                stringResource(R.string.no_matches),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val searchListState = rememberLazyListState()
            LazyColumn(
                state = searchListState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.scrollbar(searchListState),
            ) {
                items(
                    count = searchResults.itemCount,
                    key = searchResults.itemKey { it.uri.toString() },
                ) { index ->
                    val note = searchResults[index]
                    if (note != null) {
                        itemContent(note)
                    }
                }
            }
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
    supportingText: String? = null,
    isPinned: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onShowInfo: () -> Unit,
    onRename: () -> Unit,
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
                    else MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = {
                if (supportingText != null) {
                    Text(supportingText)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
    supportingText: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        content = {
            Text(
                text = name,
                style = LocalTextStyle.current.copy(
                    lineBreak = LineBreak.Paragraph,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            if (supportingText != null) {
                Text(supportingText)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        onClick = onClick,
    )
}

