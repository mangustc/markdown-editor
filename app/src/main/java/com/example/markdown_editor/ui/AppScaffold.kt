package com.example.markdown_editor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { appViewModel.onProjectSelected(it) }
    }

    var searchExpanded by remember { mutableStateOf(false) }

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.imePadding(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.project?.name ?: "Select project folder")
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { appViewModel.showCreateNoteDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Create New Note")
                    }

                    if (uiState.project != null) {
                        DockedSearchBar(
                            inputField = {
                                SearchBarDefaults.InputField(
                                    query = uiState.searchQuery,
                                    onQueryChange = { appViewModel.onSearchQueryChanged(it) },
                                    onSearch = { searchExpanded = false },
                                    expanded = searchExpanded,
                                    onExpandedChange = { searchExpanded = it },
                                    placeholder = { Text("Search notes…") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = {
                                                appViewModel.onSearchQueryChanged(
                                                    ""
                                                )
                                            }) {
                                                Icon(Icons.Default.Close, null)
                                            }
                                        }
                                    }
                                )
                            },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.searchResults.isEmpty()) {
                                Text(
                                    "No matches",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                LazyColumn {
                                    items(uiState.searchResults) { note ->
                                        NoteDrawerItem(
                                            name = note.name,
                                            selected = note.uri == uiState.activeNote?.uri,
                                            onClick = { appViewModel.onNoteSelected(note) },
                                            onDelete = {},
                                            onOpen = { appViewModel.onNoteSelected(note) },
                                            onRename = {},
                                        )
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
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (navBackStackEntry?.destination?.route == MessengerDestination::class.qualifiedName) "Quick Notes" else uiState.activeNote?.name
                                ?: "Markdown Editor",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        if (navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName) {
                            TooltipBox(
                                positionProvider =
                                    TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above
                                    ),
                                tooltip = { PlainTooltip { Text("Go back") } },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(
                                    onClick = { scope.launch { appViewModel.goBack() } },
                                    shapes = IconButtonDefaults.shapes()
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
                                        TooltipAnchorPosition.Above
                                    ),
                                tooltip = { PlainTooltip { Text("Open menu") } },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(
                                    onClick = { scope.launch { appViewModel.openDrawer() } },
                                    shapes = IconButtonDefaults.shapes()
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Open menu",
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = MessengerDestination,
                modifier = Modifier.padding(innerPadding)
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
            onConfirmCreate = { name ->
                appViewModel.onCreateNote()
                appViewModel.dismissCreateNoteDialog()
            },
            initialName = uiState.newNoteNameInput,
            onNameChange = { newName -> appViewModel.updateNewNoteName(newName) }
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
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val groupInteractionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        ListItem(
            headlineContent = {
                Text(
                    name,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                ),
            trailingContent = {
                TooltipBox(
                    positionProvider =
                        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text("Note actions") } },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Note actions",
                        )
                    }
                }
                DropdownMenuPopup(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuGroup(
                        shapes = MenuDefaults.groupShape(index = 0, count = 2),
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuDefaults.Label { Text("Actions") }
                        HorizontalDivider(
                            modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                        )

                        DropdownMenuItem(
                            text = { Text("Open note") },
                            shapes = MenuDefaults.itemShape(index = 0, count = 2),
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            selectedLeadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            selected = false,
                            onClick = { menuExpanded = false; onOpen() },
                        )

                        DropdownMenuItem(
                            text = { Text("Rename note") },
                            shapes = MenuDefaults.itemShape(index = 1, count = 2),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DriveFileRenameOutline,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            selectedLeadingIcon = {
                                Icon(
                                    Icons.Filled.DriveFileRenameOutline,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                )
                            },
                            selected = false,
                            onClick = { menuExpanded = false; onRename() },
                        )
                    }

                    Spacer(Modifier.height(MenuDefaults.GroupSpacing))

                    DropdownMenuGroup(
                        shapes = MenuDefaults.groupShape(index = 1, count = 2),
                        interactionSource = groupInteractionSource,
                    ) {
                        MenuDefaults.Label { Text("More") }
                        HorizontalDivider(
                            modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                        )

                        DropdownMenuItem(
                            text = { Text("Delete note") },
                            supportingText = { Text("Cannot be undone") },
                            shapes = MenuDefaults.itemShape(index = 1, count = 2),
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            selectedLeadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            selected = false,
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                            ),
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        )

    }
}

@Composable
fun CreateNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmCreate: (String) -> Unit,
    initialName: String,
    onNameChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("New Note") },
        text = {
            Column {
                Text("Enter a name for your new markdown file:")
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
            TextButton(
                onClick = { if (initialName.isNotBlank()) onConfirmCreate(initialName) }
            ) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
        },
    )
}