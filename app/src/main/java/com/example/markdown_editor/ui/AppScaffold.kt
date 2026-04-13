package com.example.markdown_editor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.markdown_editor.ui.editor.EditorScreen
import com.example.markdown_editor.ui.messenger.MessengerScreen
import com.example.markdown_editor.ui.navigation.AppDestination
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val appViewModel: AppViewModel = viewModel()
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { appViewModel.onProjectSelected(it) }
    }

    val displayedNotes = uiState.searchResults ?: uiState.notes
    val isSearchActive = uiState.searchQuery.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { folderPicker.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.project?.name ?: "Select project folder")
                    }
                }

                HorizontalDivider()

                // Search bar — only shown when a project is open
                if (uiState.project != null) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.searchQuery,
                                onQueryChange = { appViewModel.onSearchQueryChanged(it) },
                                onSearch = {},
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = { Text("Search…") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                },
                                trailingIcon = {
                                    if (isSearchActive) {
                                        IconButton(onClick = { appViewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {}

                    Button(
                        onClick = { appViewModel.showCreateNoteDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Create New Note")
                    }
                }

                // Notes list
                if (uiState.isLoadingNotes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (displayedNotes.isEmpty()) {
                    Text(
                        text = when {
                            uiState.project == null -> "Open a project folder to see notes"
                            uiState.searchResults != null -> "No notes match your search"
                            else -> "No notes yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    if (uiState.isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    } else {
                        NavigationDrawerItem(
                            label = { Text("Quick Notes Feed") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(AppDestination.Messenger.route) {
                                    launchSingleTop = true
                                    popUpTo(AppDestination.Editor.route) { inclusive = true }
                                }
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        LazyColumn {
                            items(displayedNotes) { note ->
                                NavigationDrawerItem(
                                    label = { Text(note.name) },
                                    selected = note.uri == uiState.activeNote?.uri,
                                    onClick = {
                                        appViewModel.onNoteSelected(note)
                                        scope.launch { drawerState.close() }
                                        navController.navigate(AppDestination.Editor.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
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
                            uiState.activeNote?.name
                                ?: "Markdown Editor"
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Open menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Editor.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestination.Editor.route) {
                    EditorScreen(viewModel = appViewModel)
                }
                composable(AppDestination.Messenger.route) {
                    // Pass necessary context/VM setup here (Requires AppViewModel to manage this state)
                    MessengerScreen(
                        viewModel = appViewModel,
                        onNavigateToEditor = { uri ->
                            navController.navigate(AppDestination.Editor.route) { /* ... */ }
                        }
                    )
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
            onNameChange = { newName ->
                appViewModel.updateNewNoteName(newName)
            }
        )
    }
}

@Composable
fun CreateNoteDialog(
    onDismissRequest: () -> Unit,
    onConfirmCreate: (String) -> Unit,
    initialName: String,
    onNameChange: (String) -> Unit
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (initialName.isNotBlank()) {
                        onConfirmCreate("Placeholder Name")
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Cancel") }
        }
    )
}