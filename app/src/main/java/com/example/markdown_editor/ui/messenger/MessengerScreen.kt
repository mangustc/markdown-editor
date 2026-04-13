package com.example.markdown_editor.ui.messenger

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(
    viewModel: MessengerViewModel = viewModel(),
    onNavigateToEditor: (Uri) -> Unit,
    project: Project?
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(project) {
        project?.let { viewModel.onMessengerOpened(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Quick Notes") },
            actions = {
                IconButton(onClick = { viewModel.onCreateNote() }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "New Note")
                }
            }
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(uiState.notesList) { note ->
                MessengerMessageItem(note = note, onClick = { onNavigateToEditor(note.uri) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MessengerMessageItem(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.text ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
