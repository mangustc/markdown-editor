package com.example.markdown_editor.ui

import android.content.ClipData
import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.markdown_editor.R
import com.example.markdown_editor.domain.PINNED_TAG
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.ui.components.NoteDrawerItem
import com.example.markdown_editor.ui.components.NoteSearchBar
import com.example.markdown_editor.ui.components.TooltipIconButton
import com.example.markdown_editor.ui.editor.EditorScreen
import com.example.markdown_editor.ui.messenger.MessengerScreen
import com.example.markdown_editor.ui.navigation.EditorDestination
import com.example.markdown_editor.ui.navigation.MessengerDestination
import com.example.markdown_editor.ui.settings.SettingsDialog
import com.example.markdown_editor.ui.util.onNotificationToast
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import com.example.markdown_editor.ui.viewmodel.events.ClipboardEvent
import com.example.markdown_editor.ui.viewmodel.events.FocusEvent
import com.example.markdown_editor.ui.viewmodel.events.NavigationEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppScaffold(
    appViewModel: AppViewModel,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { appViewModel.project.onProjectSelected(FileSystemPath(it.toString())) }
    }

    val clipboard = LocalClipboard.current
    val resources = LocalResources.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appViewModel.navigationEvents.collect {
            when (it) {
                is NavigationEvent.GoToEditor ->
                    navController.navigate(EditorDestination(it.note.projectFile.relativePath.value))

                is NavigationEvent.GoBack -> navController.popBackStack()
                is NavigationEvent.OpenDrawer -> scope.launch { drawerState.open() }
                is NavigationEvent.CloseDrawer -> scope.launch { drawerState.close() }
                is NavigationEvent.OpenUrl -> uriHandler.openUri(it.url)
                is NavigationEvent.OpenFile -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        val uri = it.uri.value.toUri()
                        setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.focusEvents.collect {
            when (it) {
                is FocusEvent.ClearFocus -> focusManager.clearFocus()
            }
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.clipboardEvents.collect {
            when (it) {
                is ClipboardEvent.Copy -> clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(
                            "Markdown Editor",
                            it.text,
                        ),
                    ),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        fun toast(message: String) {
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT,
            ).show()
        }
        appViewModel.notificationEvents.collect {
            onNotificationToast(it, ::toast, resources)
        }
    }

    val searchResults = appViewModel.drawer.searchResultsPaged.collectAsLazyPagingItems()

    val isSelectionMode = uiState.messengerSelectedNotes.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.imePadding(),
            ) {
                val reverseLayout = false
                val projectComponent = @Composable {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        SplitButtonLayout(
                            leadingButton = {
                                val projectName = uiState.project?.name
                                    ?: resources.getString(R.string.select_project_folder)
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Below,
                                    ),
                                    tooltip = { PlainTooltip { Text(projectName) } },
                                    state = rememberTooltipState(),
                                ) {
                                    val content = @Composable {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                                            contentDescription = projectName,
                                        )
                                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                        Text(projectName)
                                    }
                                    if (uiState.project != null) {
                                        SplitButtonDefaults.TonalLeadingButton(
                                            onClick = { folderPicker.launch(null) },
                                        ) { content() }
                                    } else {
                                        Button(
                                            onClick = { folderPicker.launch(null) },
                                            shapes = ButtonDefaults.shapes(),
                                        ) { content() }
                                    }
                                }
                            },
                            trailingButton = {
                                if (uiState.project != null) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                            TooltipAnchorPosition.Below,
                                        ),
                                        tooltip = { PlainTooltip { Text("Settings") } },
                                        state = rememberTooltipState(),
                                    ) {
                                        SplitButtonDefaults.TonalTrailingButton(
                                            checked = false,
                                            onCheckedChange = {
                                                appViewModel.settings.showSettings()
                                            },
                                        ) {
                                            Icon(
                                                Icons.Default.Settings,
                                                modifier = Modifier.size(SplitButtonDefaults.TrailingIconSize),
                                                contentDescription = "Settings",
                                            )
                                        }
                                    }
                                }
                            },
                        )
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below,
                            ),
                            tooltip = { PlainTooltip { Text("Sync") } },
                            state = rememberTooltipState(),
                        ) {
                            OutlinedIconButton(
                                onClick = {
                                    appViewModel.project.syncNow()
                                },
                                border = BorderStroke(
                                    width = IconButtonDefaults.outlinedIconButtonBorder(true).width,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                ),
                                shapes = IconButtonDefaults.shapes(
                                    shape = IconButtonDefaults.mediumSquareShape,
                                ),
                                modifier = Modifier.size(
                                    IconButtonDefaults.mediumContainerSize(),
                                ),
                            ) {
                                if (uiState.isSyncInProgress) {
                                    LoadingIndicator()
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync")
                                }
                            }
                        }
                    }
                }
                val searchComponent = @Composable {
                    if (uiState.project != null) {
                        NoteSearchBar(
                            searchState = appViewModel.drawer.searchState,
                            searchResults = searchResults,
                            onSearchEvent = appViewModel.drawer::onSearchEvent,
                            reverseLayout = reverseLayout,
                            paddingValues = PaddingValues(horizontal = 16.dp),
                        ) { note ->
                            NoteDrawerItem(
                                name = note.name,
                                supportingText = if (!note.tags.isNullOrEmpty()) note.tags.joinToString(
                                    ", ",
                                ) else null,
                                isPinned = note.tags?.contains(PINNED_TAG) == true,
                                selected = note.projectFile.relativePath == uiState.activeNote?.projectFile?.relativePath,
                                onClick = { appViewModel.drawer.onNoteSelected(note); focusManager.clearFocus() },
                                onOpen = { appViewModel.drawer.onNoteSelected(note) },
                                onDelete = { appViewModel.drawer.showNoteDeleteDialog(note) },
                                onRename = { appViewModel.drawer.showNoteRenameDialog(note) },
                                onShowInfo = { appViewModel.drawer.showNoteShowInfoDialog(note) },
                                onPin = { appViewModel.drawer.onPinNote(note) },
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.open_a_project_folder_to_see_notes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                val createNoteComponent = @Composable {
                    FilledTonalButton(
                        onClick = { appViewModel.drawer.showCreateNoteDialog() },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = stringResource(R.string.create_new_note),
                        )
                        Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                        Text(text = stringResource(R.string.create_new_note))
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 8.dp),
                ) {
                    if (reverseLayout) {
                        projectComponent()
                        HorizontalDivider()
                        Spacer(Modifier.weight(1f))
                        searchComponent()
                    } else {
                        searchComponent()
                        Spacer(Modifier.weight(1f))
                        HorizontalDivider()
                        createNoteComponent()
                        projectComponent()
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
                                onClick = { appViewModel.editor.onCloseEditor() },
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                tooltip = stringResource(R.string.go_back),
                                tooltipAnchorPosition = TooltipAnchorPosition.Below,
                            )
                        } else {
                            TooltipIconButton(
                                onClick = {
                                    appViewModel.onEvent(NavigationEvent.OpenDrawer)
                                },
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
                                    appViewModel.messenger.copySelectedNotesText()
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

                        if (!isSelectionMode &&
                            navBackStackEntry?.destination?.route != MessengerDestination::class.qualifiedName
                        ) {
                            if (!uiState.isViewingMode) {
                                TooltipIconButton(
                                    onClick = { appViewModel.editor.toggleViewingMode() },
                                    icon = Icons.Default.Visibility,
                                    tooltip = stringResource(R.string.read_editor),
                                    tooltipAnchorPosition = TooltipAnchorPosition.Below,
                                )
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
                    EditorScreen(viewModel = appViewModel, noteRelativePath = data.noteRelativePath)
                }
                composable<MessengerDestination> {
                    MessengerScreen(viewModel = appViewModel)
                }
            }
        }
    }

    if (uiState.isCreateNoteDialogVisible) {
        CreateNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissCreateNoteDialog() },
            onConfirmCreate = {
                appViewModel.drawer.onCreateNote()
                appViewModel.drawer.dismissCreateNoteDialog()
            },
            initialName = uiState.newNoteNameInput,
            onNameChange = { newName -> appViewModel.drawer.updateNewNoteName(newName) },
        )
    }
    if (uiState.isNoteDeleteDialogVisible && uiState.dialogNote != null) {
        DeleteNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteDeleteDialog() },
            onConfirmDelete = {
                appViewModel.drawer.onDeleteNote(uiState.dialogNote!!)
                appViewModel.drawer.dismissNoteDeleteDialog()
            },
            noteName = uiState.dialogNote!!.name,
        )
    }
    if (uiState.isNoteRenameDialogVisible && uiState.dialogNote != null) {
        RenameNoteDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteRenameDialog() },
            onConfirmRename = {
                appViewModel.drawer.onRenameNote(uiState.dialogNote!!, uiState.noteRenameInput)
                appViewModel.drawer.dismissNoteRenameDialog()
            },
            name = uiState.noteRenameInput,
            onNameChange = { newName -> appViewModel.drawer.onRenameNameInputChanged(newName) },
        )
    }
    if (uiState.isNoteShowInfoDialogVisible && uiState.dialogNote != null) {
        ShowInfoDialog(
            onDismissRequest = { appViewModel.drawer.dismissNoteShowInfoDialog() },
            note = uiState.dialogNote!!,
        )
    }
    if (uiState.isSettingsDialogVisible && uiState.settings != null) {
        SettingsDialog(
            onDismissRequest = { appViewModel.settings.dismissSettings() },
            settings = uiState.settings!!,
            onSyncProviderChange = appViewModel.settings::setSyncProvider,
            onOauthTokenChange = appViewModel.settings::setYandexOauthToken,
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
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
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
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
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