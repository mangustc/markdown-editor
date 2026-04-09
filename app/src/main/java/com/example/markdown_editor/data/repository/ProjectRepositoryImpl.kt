package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val context: Context,
    private val prefs: SharedPreferences
) : ProjectRepository {

    override fun getNotes(project: Project): Flow<List<Note>> = flow {
        val notesDir = DocumentFile.fromTreeUri(context, project.notesUri)
        val notes = notesDir
            ?.listFiles()
            ?.filter { it.name?.endsWith(".md") == true }
            ?.map { file ->
                Note(
                    name = file.name?.removeSuffix(".md") ?: "Untitled",
                    uri = file.uri,
                    lastModified = file.lastModified()
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
        emit(notes)
    }.flowOn(Dispatchers.IO)

    override suspend fun saveProject(project: Project) {
        // Persist the URI so it survives app restart
        // takePersistableUriPermission ensures we can access it after reboot
        context.contentResolver.takePersistableUriPermission(
            project.uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit()
            .putString(KEY_PROJECT_URI, project.uri.toString())
            .putString(KEY_PROJECT_NAME, project.name)
            .apply()
    }

    override suspend fun loadSavedProject(): Project? = withContext(Dispatchers.IO) {
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return@withContext null
        val name = prefs.getString(KEY_PROJECT_NAME, null) ?: return@withContext null
        val uri = Uri.parse(uriString)
        buildProject(uri, name)
    }

    override suspend fun createNote(project: Project, name: String): Uri? =
        withContext(Dispatchers.IO) {
            val notesDir = DocumentFile.fromTreeUri(context, project.notesUri)
            notesDir?.createFile("text/markdown", "$name.md")?.uri
        }

    // Derives notes/ and assets/ URIs from the root project URI
    fun buildProject(rootUri: Uri, name: String): Project {
        val root = DocumentFile.fromTreeUri(context, rootUri)
        val notesDir = root?.findFile("notes")
            ?: root?.createDirectory("notes")
        val assetsDir = root?.findFile("assets")
            ?: root?.createDirectory("assets")
        return Project(
            name = name,
            uri = rootUri,
            notesUri = notesDir?.uri ?: rootUri,
            assetsUri = assetsDir?.uri ?: rootUri
        )
    }

    companion object {
        private const val KEY_PROJECT_URI = "project_uri"
        private const val KEY_PROJECT_NAME = "project_name"
    }
}