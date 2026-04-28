package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.LinkPreviewEntity
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteEntity
import com.example.markdown_editor.data.model.LinkPreview
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import com.example.markdown_editor.data.model.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val context: Context,
    private val noteDao: NoteDao,
    private val linkPreviewDao: LinkPreviewDao,
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
                val (fmString, body) = splitFrontMatter(fullText)
                val frontMatter = parseFrontMatter(fmString)
                val tags = (frontMatter["tags"] as? List<*>)?.joinToString(" ") ?: ""

                val entity = NoteEntity(
                    id = cached?.id ?: 0,
                    uri = uriStr,
                    name = file.name?.removeSuffix(".md") ?: "Untitled",
                    lastModified = file.lastModified(),
                    createdAt = getCreatedAt(frontMatter),
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
        prefs.edit()
            .putString(KEY_PROJECT_URI, project.uri.toString())
            .putString(KEY_PROJECT_NAME, project.name)
            .apply()
    }

    override suspend fun loadSavedProject(): Project? = withContext(Dispatchers.IO) {
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return@withContext null
        val name = prefs.getString(KEY_PROJECT_NAME, null) ?: return@withContext null
        buildProject(uriString.toUri(), name)
    }

    override suspend fun createNote(project: Project, name: String?, tags: List<String>?): Uri? =
        withContext(Dispatchers.IO) {
            val notesDir =
                DocumentFile.fromTreeUri(context, project.notesUri) ?: return@withContext null

            val isoDate = java.time.Instant.now().toString()
            var frontMatterBuilder = "---\ncreatedAt: $isoDate"
            if (!tags.isNullOrEmpty()) {
                frontMatterBuilder += "\ntags:"
                tags.forEach { tag -> frontMatterBuilder += "\n- $tag" }
            }
            val initialContent = "$frontMatterBuilder\n---"

            val newFile =
                notesDir.createFile("text/markdown", "$name.md") ?: return@withContext null
            context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { out ->
                out.bufferedWriter().use { it.write(initialContent) }
            }
            newFile.uri
        }

    override suspend fun getNoteText(note: Note, includeFrontMatter: Boolean): String =
        withContext(Dispatchers.IO) {
            val fullText = context.contentResolver
                .openInputStream(note.uri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: ""
            if (includeFrontMatter) fullText else splitFrontMatter(fullText).second
        }

    override suspend fun saveNoteText(note: Note, text: String): Note =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(note.uri, "wt")
                ?.bufferedWriter()
                ?.use { it.write(text) }
            note
        }

    override suspend fun getNoteByUri(uri: Uri): Note = withContext(Dispatchers.IO) {
        val content = try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            return@withContext Note(
                name = "Error",
                uri = uri,
                lastModified = System.currentTimeMillis(),
            )
        }

        val (frontMatterString, _) = splitFrontMatter(content)
        val frontMatter = parseFrontMatter(frontMatterString)
        val createdAtLong = getCreatedAt(frontMatter)
        val documentFile = DocumentFile.fromSingleUri(context, uri)
            ?: return@withContext Note(name = "Unknown", uri = uri, lastModified = 0L)

        Note(
            name = documentFile.name?.removeSuffix(".md") ?: "Untitled",
            uri = uri,
            lastModified = documentFile.lastModified(),
            createdAt = createdAtLong,
        )
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

    override suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        val file = DocumentFile.fromSingleUri(context, note.uri)
        if (file?.exists() == true) file.delete()
    }

    override suspend fun renameNote(note: Note, newName: String) = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(
            context.contentResolver,
            note.uri,
            "$newName.md",
        ) ?: throw IllegalStateException("Failed to rename file in storage")
        Unit
    }

    override suspend fun getCachedLinkPreview(url: String): LinkPreview? =
        withContext(Dispatchers.IO) {
            linkPreviewDao.getByUrl(url)?.let { entity ->
                // All-null fields = previously attempted fetch that failed → return null
                // so the caller can decide whether to retry
                if (entity.title == null && entity.description == null && entity.imageUrl == null)
                    null
                else
                    LinkPreview(
                        url = entity.url,
                        title = entity.title,
                        description = entity.description,
                        imageUrl = entity.imageUrl,
                    )
            }
        }

    override suspend fun saveLinkPreview(preview: LinkPreview) = withContext(Dispatchers.IO) {
        linkPreviewDao.insert(
            LinkPreviewEntity(
                url = preview.url,
                title = preview.title,
                description = preview.description,
                imageUrl = preview.imageUrl,
            ),
        )
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun splitFrontMatter(content: String): Pair<String, String> {
        if (!content.trimStart().startsWith("---")) return "" to content
        val lines = content.lines()
        val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (closeIdx < 0) return "" to content
        val fmLines = lines.drop(1).take(closeIdx)
        val bodyLines = lines.drop(closeIdx + 2)
        return fmLines.joinToString("\n") to bodyLines.joinToString("\n")
    }

    private fun parseFrontMatter(text: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val lines = text.trim().lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#")) {
                i++; continue
            }

            val kv = line.split(":", limit = 2).map { it.trim() }
            if (kv.size != 2) throw IllegalArgumentException("Invalid line format: $line")

            val key = kv[0]
            val valuePart = kv[1]

            if (valuePart.isEmpty()) {
                val list = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].trimStart().startsWith("-")) {
                    list.add(lines[i].trim().removePrefix("-").trim())
                    i++
                }
                result[key] = list.toList()
            } else {
                result[key] = valuePart
                i++
            }
        }
        return result
    }

    private fun getCreatedAt(frontMatter: Map<String, Any>): Long? =
        frontMatter["createdAt"]?.let { value ->
            try {
                java.time.Instant.parse(value as String).toEpochMilli()
            } catch (e: Exception) {
                null
            }
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
