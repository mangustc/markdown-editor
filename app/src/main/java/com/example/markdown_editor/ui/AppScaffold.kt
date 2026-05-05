package com.example.markdown_editor.ui

import android.content.ClipData
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.markdown_editor.R
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.ui.components.MenuPopup
import com.example.markdown_editor.ui.components.MenuPopupGroup
import com.example.markdown_editor.ui.components.MenuPopupItem
import com.example.markdown_editor.ui.components.TooltipIconButton
import com.example.markdown_editor.ui.editor.EditorScreen
import com.example.markdown_editor.ui.messenger.MessengerScreen
import com.example.markdown_editor.ui.navigation.EditorDestination
import com.example.markdown_editor.ui.navigation.MessengerDestination
import com.example.markdown_editor.ui.util.scrollbar
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val appViewModel: AppViewModel = viewModel()
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { appViewModel.project.onProjectSelected(it) }
    }

    LaunchedEffect(Unit) {
        appViewModel.navigation.navigationEvents.collect { event ->
            when (event) {
                is AppViewModel.NavigationEvent.GoToEditor ->
                    navController.navigate(EditorDestination(event.note.uri.toString()))

                is AppViewModel.NavigationEvent.GoBack -> navController.popBackStack()
                is AppViewModel.NavigationEvent.OpenDrawer -> scope.launch { drawerState.open() }
                is AppViewModel.NavigationEvent.CloseDrawer -> scope.launch { drawerState.close() }
            }
        }
    }
    val searchResults = appViewModel.navigation.searchResultsPaged.collectAsLazyPagingItems()

    val clipboard = LocalClipboard.current
    val isSelectionMode = uiState.messengerSelectedNotes.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.imePadding(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            uiState.project?.name ?: stringResource(R.string.select_project_folder),
                        )
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { appViewModel.navigation.showCreateNoteDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.create_new_note))
                    }

                    if (uiState.project != null) {
                        DockedSearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = uiState.searchQuery,
                                    onQueryChange = {
                                        appViewModel.navigation.onSearchQueryChanged(
                                            it,
                                        )
                                    },
                                    onSearch = {},
                                    expanded = true,
                                    onExpandedChange = {},
                                    placeholder = { Text(stringResource(R.string.search_notes)) },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            TooltipIconButton(
                                                onClick = {
                                                    appViewModel.navigation.onSearchQueryChanged(
                                                        "",
                                                    )
                                                },
                                                icon = Icons.Default.Close,
                                                tooltip = stringResource(R.string.clear_search),
                                            )
                                        }
                                    },
                                )
                            },
                            expanded = true,
                            onExpandedChange = {},
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                                    modifier = Modifier
                                        .scrollbar(searchListState),
                                ) {
                                    items(
                                        count = searchResults.itemCount,
                                        key = searchResults.itemKey { it.uri.toString() },
                                    ) { index ->
                                        val note = searchResults[index]
                                        if (note != null) {
                                            NoteDrawerItem(
                                                name = note.name,
                                                supportingText = if (!note.tags.isNullOrEmpty()) note.tags.joinToString(
                                                    ", ",
                                                ) else null,
                                                isPinned = note.tags?.contains("pinned") == true,
                                                selected = note.uri == uiState.activeNote?.uri,
                                                onClick = {
                                                    appViewModel.navigation.onNoteSelected(
                                                        note,
                                                    )
                                                },
                                                onOpen = {
                                                    appViewModel.navigation.onNoteSelected(
                                                        note,
                                                    )
                                                },
                                                onDelete = {
                                                    appViewModel.navigation.showNoteDeleteDialog(
                                                        note,
                                                    )
                                                },
                                                onRename = {
                                                    appViewModel.navigation.showNoteRenameDialog(
                                                        note,
                                                    )
                                                },
                                                onShowInfo = {
                                                    appViewModel.navigation.showNoteShowInfoDialog(
                                                        note,
                                                    )
                                                },
                                                onPin = { appViewModel.navigation.onPinNote(note) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            stringResource(R.string.open_a_project_folder_to_see_notes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text("${uiState.messengerSelectedNotes.size}")
                        } else {
                            Text(
                                if (navBackStackEntry?.destination?.route == MessengerDestination::class.qualifiedName) stringResource(
                                    R.string.quick_notes,
                                ) else uiState.activeNote?.name
                                    ?: stringResource(R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        if (isSelectionMode) {
                            TooltipIconButton(
                                onClick = { appViewModel.messenger.clearSelection() },
                                icon = Icons.Default.Close,
                                tooltip = stringResource(R.string.clear_selection),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        } else if (navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName) {
                            TooltipIconButton(
                                onClick = { scope.launch { appViewModel.navigation.goBack() } },
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                tooltip = stringResource(R.string.go_back),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        } else {
                            TooltipIconButton(
                                onClick = { scope.launch { appViewModel.navigation.openDrawer() } },
                                icon = Icons.Default.Menu,
                                tooltip = stringResource(R.string.open_menu),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            TooltipIconButton(
                                onClick = {
                                    scope.launch {
                                        val text = appViewModel.messenger.getSelectedNotesText()
                                        clipboard.setClipEntry(
                                            ClipEntry(
                                                ClipData.newPlainText(
                                                    "Notes text",
                                                    text,
                                                ),
                                            ),
                                        )
                                        appViewModel.messenger.clearSelection()
                                    }
                                },
                                icon = Icons.Outlined.ContentCopy,
                                tooltip = stringResource(R.string.copy_selected),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                            TooltipIconButton(
                                onClick = { appViewModel.messenger.deleteSelectedNotes() },
                                icon = Icons.Outlined.Delete,
                                tooltip = stringResource(R.string.delete_selected),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MessengerDestination,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<EditorDestination> { backStackEntry ->
                    val data: EditorDestination = backStackEntry.toRoute()
                    EditorScreen(viewModel = appViewModel, noteUriString = data.noteUriString)
                }
                composable<MessengerDestination> {
                    MessengerScreen(viewModel = appViewModel)
                }
            }
        }
    }

    if (uiState.isCreateNoteDialogVisible) {
        CreateNoteDialog(
            onDismissRequest = { appViewModel.navigation.dismissCreateNoteDialog() },
            onConfirmCreate = {
                appViewModel.navigation.onCreateNote()
                appViewModel.navigation.dismissCreateNoteDialog()
            },
            initialName = uiState.newNoteNameInput,
            onNameChange = { newName -> appViewModel.navigation.updateNewNoteName(newName) },
        )
    }
    if (uiState.isNoteDeleteDialogVisible && uiState.dialogNote != null) {
        DeleteNoteDialog(
            onDismissRequest = { appViewModel.navigation.dismissNoteDeleteDialog() },
            onConfirmDelete = {
                appViewModel.navigation.onDeleteNote(uiState.dialogNote!!)
                appViewModel.navigation.dismissNoteDeleteDialog()
            },
            noteName = uiState.dialogNote!!.name,
        )
    }
    if (uiState.isNoteRenameDialogVisible && uiState.dialogNote != null) {
        RenameNoteDialog(
            onDismissRequest = { appViewModel.navigation.dismissNoteRenameDialog() },
            onConfirmRename = {
                appViewModel.navigation.onRenameNote(uiState.dialogNote!!, uiState.noteRenameInput)
                appViewModel.navigation.dismissNoteRenameDialog()
            },
            name = uiState.noteRenameInput,
            onNameChange = { newName -> appViewModel.navigation.onRenameNameInputChanged(newName) },
        )
    }
    if (uiState.isNoteShowInfoDialogVisible && uiState.dialogNote != null) {
        ShowInfoDialog(
            onDismissRequest = { appViewModel.navigation.dismissNoteShowInfoDialog() },
            note = uiState.dialogNote!!,
        )
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

@Composable
fun CreateNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmCreate: () -> Unit,
    initialName: String,
    onNameChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.new_note)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_a_name_for_your_new_note))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialName,
                    onValueChange = { onNameChange(it) },
                    label = { Text(stringResource(R.string.note_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (initialName.isNotBlank()) onConfirmCreate() },
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun RenameNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmRename: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.rename_note)) },
        text = {
            Column {
                Text(stringResource(R.string.enter_a_new_name_for_your_note))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it) },
                    label = { Text(stringResource(R.string.note_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirmRename) { Text(stringResource(R.string.rename)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun DeleteNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    noteName: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.delete_note)) },
        text = {
            Column {
                Text(stringResource(R.string.are_you_sure_you_want_to_delete_this_note))
                Text(
                    text = noteName,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.this_action_cannot_be_undone),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun ShowInfoDialog(
    onDismissRequest: () -> Unit,
    note: Note,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.note_details)) },
        text = {
            Column {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.name)) },
                    supportingContent = { Text(note.name) },
                    leadingContent = { Icon(Icons.Default.Abc, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.last_modified)) },
                    supportingContent = {
                        val timeString = DateUtils.getRelativeTimeSpanString(
                            note.lastModified,
                            System.currentTimeMillis(),
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE,
                        )
                        val result =
                            if (!timeString.isNullOrBlank()) timeString else stringResource(R.string.n_a)
                        Text(result.toString())
                    },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.created_at)) },
                    supportingContent = {
                        val timeString =
                            if (note.createdAt != null) DateUtils.getRelativeTimeSpanString(
                                note.createdAt,
                                System.currentTimeMillis(),
                                DateUtils.SECOND_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) else null
                        val result =
                            if (!timeString.isNullOrBlank()) timeString else stringResource(
                                R.string.n_a,
                            )
                        Text(result.toString())
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                        )
                    },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(stringResource(R.string.tags)) },
                    supportingContent = {
                        Text(
                            if (!note.tags.isNullOrEmpty()) note.tags.joinToString(", ") else stringResource(
                                R.string.none,
                            ),
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) { Text(stringResource(R.string.close)) }
        },
    )
}
