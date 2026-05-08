package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteEntity
import com.example.markdown_editor.data.model.FrontMatter
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.model.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val context: Context,
    private val noteDao: NoteDao,
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "project_prefs",
        Context.MODE_PRIVATE,
    ),
) : ProjectRepository {
    override suspend fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): List<Note> = withContext(Dispatchers.IO) {
        val sqlQuery = buildSQLiteQuery(query)
        val entities = noteDao.searchNotes(sqlQuery)

        entities.map { entity ->
            Note(
                name = entity.name,
                uri = entity.uri.toUri(),
                lastModified = entity.lastModified,
                createdAt = entity.createdAt,
                body = if (includeText) entity.body else null,
                tags = if (entity.tags.isNotEmpty()) entity.tags.split(" ") else emptyList(),
            )
        }
    }

    override fun getNotesPaged(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Flow<PagingData<Note>> {
        val sqlQuery = buildSQLiteQuery(query)
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 20,
                enablePlaceholders = false,
            ),
        ) {
            noteDao.searchNotesPaged(sqlQuery)
        }.flow.map { pagingData ->
            pagingData.map { entity ->
                Note(
                    name = entity.name,
                    uri = entity.uri.toUri(),
                    lastModified = entity.lastModified,
                    createdAt = entity.createdAt,
                    body = if (includeText) entity.body else null,
                    tags = if (entity.tags.isNotEmpty()) entity.tags.split(" ") else emptyList(),
                )
            }
        }
    }

    override suspend fun syncDatabase(project: Project) = withContext(Dispatchers.IO) {
        val notesDir = DocumentFile.fromTreeUri(context, project.notesUri)
        val files =
            notesDir?.listFiles()?.filter { it.name?.endsWith(".md") == true }
                ?: return@withContext

        val existingNotes = noteDao.searchNotes(buildSQLiteQuery(SearchQuery()))
        val existingUris = existingNotes.associateBy { it.uri }

        files.forEach { file ->
            val uriStr = file.uri.toString()
            val cached = existingUris[uriStr]

            if (cached == null || file.lastModified() > cached.lastModified) {
                val fullText = readFullText(file.uri)
                val (frontMatter, body) = FrontMatter.splitFromContent(fullText)
                val tags = frontMatter.toTagString()

                val entity = NoteEntity(
                    id = cached?.id ?: 0,
                    uri = uriStr,
                    name = file.name?.removeSuffix(".md") ?: "Untitled",
                    lastModified = file.lastModified(),
                    createdAt = frontMatter.toCreatedAtMillis(),
                    tags = tags,
                    body = body,
                )
                noteDao.insertNote(entity)
            }
        }

        val currentFileUris = files.map { it.uri.toString() }.toSet()
        existingNotes.forEach { cached ->
            if (cached.uri !in currentFileUris) noteDao.deleteByUri(cached.uri)
        }
    }

    override fun buildProject(rootUri: Uri, name: String): Project {
        val root = DocumentFile.fromTreeUri(context, rootUri)
        val notesDir = root?.findFile("notes") ?: root?.createDirectory("notes")
        val assetsDir = root?.findFile("assets") ?: root?.createDirectory("assets")
        return Project(
            name = name,
            uri = rootUri,
            notesPath = if (notesDir != null) "notes" else "",
            assetsPath = if (assetsDir != null) "assets" else "",
        )
    }

    override suspend fun saveProject(project: Project) {
        context.contentResolver.takePersistableUriPermission(
            project.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        prefs.edit {
            putString(KEY_PROJECT_URI, project.uri.toString())
                .putString(KEY_PROJECT_NAME, project.name)
        }
    }

    override suspend fun loadSavedProject(): Project? = withContext(Dispatchers.IO) {
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return@withContext null
        val name = prefs.getString(KEY_PROJECT_NAME, null) ?: return@withContext null
        buildProject(uriString.toUri(), name)
    }

    override suspend fun copyToAssets(project: Project, assetUri: Uri): String =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val sourceFile = DocumentFile.fromSingleUri(context, assetUri)
            val fileName = sourceFile?.name ?: "attachment_${System.currentTimeMillis()}"
            val mimeType = resolver.getType(assetUri) ?: "application/octet-stream"

            val assetsDir = DocumentFile.fromTreeUri(context, project.assetsUri)
                ?: throw IllegalStateException("Could not access assets directory")
            val targetFile = assetsDir.createFile(mimeType, fileName)
                ?: throw IllegalStateException("Failed to create file in assets")

            try {
                resolver.openInputStream(assetUri)?.use { input ->
                    resolver.openOutputStream(targetFile.uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                targetFile.delete()
                throw e
            }

            "assets/${targetFile.name}"
        }

    private fun readFullText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""

    private fun buildSQLiteQuery(query: SearchQuery): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val sb = StringBuilder()
        val hasFts = query.bodyTerms.isNotEmpty()
        val ftsMatchExpr = query.buildFtsMatchQuery()

        sb.append("SELECT notes.* FROM notes")
        if (hasFts && ftsMatchExpr != null) {
            sb.append("\nJOIN notesFts ON notes.rowid = notesFts.rowid")
            sb.append("\n  AND notesFts MATCH ?")
            args.add(ftsMatchExpr)
        }

        val conditions = mutableListOf<String>()
        for (term in query.negatedBodyTerms) {
            conditions.add("notes.body NOT LIKE ?")
            args.add("%$term%")
        }
        for (tag in query.positiveTagLikes()) {
            conditions.add("notes.tags LIKE ?")
            args.add("%$tag%")
        }
        for (tag in query.negatedTagLikes()) {
            conditions.add("notes.tags NOT LIKE ?")
            args.add("%$tag%")
        }
        query.nameFilter?.let {
            conditions.add("notes.name LIKE ?")
            args.add("%$it%")
        }
        query.negatedNameFilter?.let {
            conditions.add("notes.name NOT LIKE ?")
            args.add("%$it%")
        }
        if (conditions.isNotEmpty()) {
            sb.append("\nWHERE ")
            sb.append(conditions.joinToString("\n  AND "))
        }

        val pinnedClause = if (query.pinnedFirst)
            "CASE WHEN notes.tags LIKE '%pinned%' THEN 0 ELSE 1 END ASC,\n  "
        else ""
        val sortClause = when (query.sortBy) {
            SortBy.LAST_MODIFIED -> "notes.lastModified DESC"
            SortBy.CREATED_AT ->
                "notes.createdAt DESC, notes.lastModified DESC"
        }
        sb.append("\nORDER BY $pinnedClause$sortClause")

        return SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
    }

    companion object {
        private const val KEY_PROJECT_URI = "project_uri"
        private const val KEY_PROJECT_NAME = "project_name"
    }
}
