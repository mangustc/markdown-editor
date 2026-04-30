package com.example.markdown_editor.ui

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTooltipState
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
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.ui.components.MenuPopup
import com.example.markdown_editor.ui.components.MenuPopupGroup
import com.example.markdown_editor.ui.components.MenuPopupItem
import com.example.markdown_editor.ui.editor.EditorScreen
import com.example.markdown_editor.ui.messenger.MessengerScreen
import com.example.markdown_editor.ui.navigation.EditorDestination
import com.example.markdown_editor.ui.navigation.MessengerDestination
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
        uri?.let { appViewModel.onProjectSelected(it) }
    }

    LaunchedEffect(Unit) {
        appViewModel.navigationEvents.collect { event ->
            when (event) {
                is AppViewModel.NavigationEvent.GoToEditor ->
                    navController.navigate(EditorDestination(event.note.uri.toString()))

                is AppViewModel.NavigationEvent.GoBack -> navController.popBackStack()
                is AppViewModel.NavigationEvent.OpenDrawer -> scope.launch { drawerState.open() }
                is AppViewModel.NavigationEvent.CloseDrawer -> scope.launch { drawerState.close() }
            }
        }
    }
    val searchResults = appViewModel.searchResultsPaged.collectAsLazyPagingItems()

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
                        Text(uiState.project?.name ?: "Select project folder")
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { appViewModel.showCreateNoteDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Create New Note")
                    }

                    if (uiState.project != null) {
                        DockedSearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = uiState.searchQuery,
                                    onQueryChange = { appViewModel.onSearchQueryChanged(it) },
                                    onSearch = {},
                                    expanded = true,
                                    onExpandedChange = {},
                                    placeholder = { Text("Search notes…") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            TooltipBox(
                                                positionProvider =
                                                    TooltipDefaults.rememberTooltipPositionProvider(
                                                        TooltipAnchorPosition.Above,
                                                    ),
                                                tooltip = { PlainTooltip { Text("Clear search") } },
                                                state = rememberTooltipState(),
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        appViewModel.onSearchQueryChanged("")
                                                    },
                                                ) {
                                                    Icon(Icons.Default.Close, "Clear search")
                                                }
                                            }
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
                                    "No matches",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
                                                onClick = { appViewModel.onNoteSelected(note) },
                                                onOpen = { appViewModel.onNoteSelected(note) },
                                                onDelete = { appViewModel.showNoteDeleteDialog(note) },
                                                onRename = { appViewModel.showNoteRenameDialog(note) },
                                                onShowInfo = {
                                                    appViewModel.showNoteShowInfoDialog(
                                                        note,
                                                    )
                                                },
                                                onPin = { appViewModel.onPinNote(note) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Open project folder to see notes",
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
                        Text(
                            if (navBackStackEntry?.destination?.route == MessengerDestination::class.qualifiedName) "Quick Notes" else uiState.activeNote?.name
                                ?: "Markdown Editor",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName) {
                            TooltipBox(
                                positionProvider =
                                    TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above,
                                    ),
                                tooltip = { PlainTooltip { Text("Go back") } },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(
                                    onClick = { scope.launch { appViewModel.goBack() } },
                                    shapes = IconButtonDefaults.shapes(),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Go back",
                                    )
                                }
                            }
                        } else {
                            TooltipBox(
                                positionProvider =
                                    TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above,
                                    ),
                                tooltip = { PlainTooltip { Text("Open menu") } },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(
                                    onClick = { scope.launch { appViewModel.openDrawer() } },
                                    shapes = IconButtonDefaults.shapes(),
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Open menu",
                                    )
                                }
                            }
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
            onDismissRequest = { appViewModel.dismissCreateNoteDialog() },
            onConfirmCreate = {
                appViewModel.onCreateNote()
                appViewModel.dismissCreateNoteDialog()
            },
            initialName = uiState.newNoteNameInput,
            onNameChange = { newName -> appViewModel.updateNewNoteName(newName) },
        )
    }
    if (uiState.isNoteDeleteDialogVisible && uiState.dialogNote != null) {
        DeleteNoteDialog(
            onDismissRequest = { appViewModel.dismissNoteDeleteDialog() },
            onConfirmDelete = {
                appViewModel.onDeleteNote(uiState.dialogNote!!)
                appViewModel.dismissNoteDeleteDialog()
            },
            noteName = uiState.dialogNote!!.name,
        )
    }
    if (uiState.isNoteRenameDialogVisible && uiState.dialogNote != null) {
        RenameNoteDialog(
            onDismissRequest = { appViewModel.dismissNoteRenameDialog() },
            onConfirmRename = {
                appViewModel.onRenameNote(uiState.dialogNote!!, uiState.noteRenameInput)
                appViewModel.dismissNoteRenameDialog()
            },
            name = uiState.noteRenameInput,
            onNameChange = { newName -> appViewModel.onRenameNameInputChanged(newName) },
        )
    }
    if (uiState.isNoteShowInfoDialogVisible && uiState.dialogNote != null) {
        ShowInfoDialog(
            onDismissRequest = { appViewModel.dismissNoteShowInfoDialog() },
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
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Note actions") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Note actions",
                        )
                    }
                }
                MenuPopup(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) { groupInteractionSource ->
                    MenuPopupGroup(
                        index = 0,
                        count = 2,
                        label = "Actions",
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuPopupItem(
                            text = "Open",
                            index = 0, count = 4,
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = { menuExpanded = false; onOpen() },
                        )

                        MenuPopupItem(
                            text = "Rename",
                            index = 1, count = 4,
                            icon = Icons.Outlined.DriveFileRenameOutline,
                            onClick = { menuExpanded = false; onRename() },
                        )

                        MenuPopupItem(
                            text = if (isPinned) "Unpin" else "Pin",
                            index = 1, count = 4,
                            icon = Icons.Default.PushPin,
                            onClick = { menuExpanded = false; onPin() },
                        )

                        MenuPopupItem(
                            text = "Delete",
                            index = 2, count = 4,
                            supportingText = "Cannot be undone",
                            icon = Icons.Outlined.Delete,
                            tint = MaterialTheme.colorScheme.error,
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }

                    Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                    MenuPopupGroup(
                        index = 1,
                        count = 2,
                        label = "More",
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuPopupItem(
                            text = "Show details",
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
        title = { Text("New Note") },
        text = {
            Column {
                Text("Enter a name for your new note:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = initialName,
                    onValueChange = { onNameChange(it) },
                    label = { Text("Note Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (initialName.isNotBlank()) onConfirmCreate() },
            ) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
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
        title = { Text("Rename note") },
        text = {
            Column {
                Text("Enter a new name for your note:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it) },
                    label = { Text("Note Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirmRename) { Text("Rename") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
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
        title = { Text("Delete note") },
        text = {
            Column {
                Text("Are you sure you want to delete this note?")
                Text(
                    text = noteName,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "This action cannot be undone",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
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
        title = { Text("Note Details") },
        text = {
            Column {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Name") },
                    supportingContent = { Text(note.name) },
                    leadingContent = { Icon(Icons.Default.Abc, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Last Modified") },
                    supportingContent = {
                        val timeString = DateUtils.getRelativeTimeSpanString(
                            note.lastModified,
                            System.currentTimeMillis(),
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE,
                        )
                        val result = if (!timeString.isNullOrBlank()) timeString else "N/A"
                        Text(result.toString())
                    },
                    leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                )
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text("Created At") },
                    supportingContent = {
                        val timeString =
                            if (note.createdAt != null) DateUtils.getRelativeTimeSpanString(
                                note.createdAt,
                                System.currentTimeMillis(),
                                DateUtils.SECOND_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) else null
                        val result = if (!timeString.isNullOrBlank()) timeString else "N/A"
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
                    headlineContent = { Text("Tags") },
                    supportingContent = {
                        Text(if (!note.tags.isNullOrEmpty()) note.tags.joinToString(", ") else "None")
                    },
                    leadingContent = { Icon(Icons.Default.Tag, contentDescription = null) },
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Close") }
        },
    )
}
