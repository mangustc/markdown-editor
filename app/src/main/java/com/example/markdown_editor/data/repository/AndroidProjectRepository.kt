package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.MimeTypeMap
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
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidProjectRepository(
    private val context: Context,
    private val noteDao: NoteDao,
) : ProjectRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "project_prefs",
        Context.MODE_PRIVATE,
    )

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

    override fun buildProject(rootUri: Uri): Project {
        val root = DocumentFile.fromTreeUri(context, rootUri)
        val notesDir = root?.findFile("notes") ?: root?.createDirectory("notes")
        val assetsDir = root?.findFile("assets") ?: root?.createDirectory("assets")
        return Project(
            name = root?.name ?: "Project",
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
        }
    }

    override suspend fun loadSavedProject(): Project? = withContext(Dispatchers.IO) {
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return@withContext null
        buildProject(uriString.toUri())
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

    override suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        noteDao.getAllTags().flatMap { it.split(" ") }.filter { it.isNotBlank() }.distinct()
    }

    override suspend fun writeFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        overwrite: Boolean,
        createParents: Boolean,
    ): ProjectFile? = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext null

        val fileName = relativePath.basename
        val dirParts = relativePath.dirRelativePath.splitParts()

        var currentDir = rootDoc
        for (part in dirParts) {
            if (part.isEmpty()) continue
            var nextDir = currentDir.findFile(part)
            if (nextDir == null) {
                if (createParents) {
                    nextDir = currentDir.createDirectory(part) ?: return@withContext null
                } else {
                    return@withContext null
                }
            } else if (!nextDir.isDirectory) {
                return@withContext null
            }
            currentDir = nextDir
        }

        var fileDoc = currentDir.findFile(fileName)
        if (fileDoc != null) {
            if (fileDoc.isDirectory) {
                return@withContext null
            }
            if (!overwrite) {
                return@withContext null
            }
        } else {
            val extension = fileName.substringAfterLast('.', "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
            fileDoc = currentDir.createFile(mimeType, fileName) ?: return@withContext null
        }

        try {
            context.contentResolver.openOutputStream(fileDoc.uri, "wt")?.use { outputStream ->
                outputStream.write(byteArray)
            } ?: return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        ProjectFile(
            fileSystemPath = FileSystemPath(fileDoc.uri.toString()),
            relativePath = relativePath,
        )
    }

    override suspend fun deleteFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
    ) = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext

        if (relativePath.value.isEmpty()) {
            return@withContext
        }

        val targetDoc = findDocumentFile(rootDoc, relativePath) ?: return@withContext
        try {
            targetDoc.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun readFile(
        project: com.example.markdown_editor.domain.models.Project,
        relativePath: RelativePath,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext null

        val targetDoc = findDocumentFile(rootDoc, relativePath) ?: return@withContext null

        if (!targetDoc.isFile) {
            return@withContext null
        }

        try {
            context.contentResolver.openInputStream(targetDoc.uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getProjectFilesList(
        project: com.example.markdown_editor.domain.models.Project,
    ): List<ProjectFile> = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()

        val result = mutableListOf<ProjectFile>()
        walkDocumentTree(rootDoc, RelativePath(""), result)
        return@withContext result.toList()
    }

    private fun walkDocumentTree(
        dir: DocumentFile,
        prefix: RelativePath,
        out: MutableList<ProjectFile>,
    ) {
        dir.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            val relPath = prefix.appendRelativePath(RelativePath(name))
            if (file.isDirectory) {
                walkDocumentTree(file, relPath, out)
            } else {
                out.add(
                    ProjectFile(
                        fileSystemPath = FileSystemPath(file.uri.toString()),
                        relativePath = relPath,
                    ),
                )
            }
        }
    }

    private fun findDocumentFile(rootDoc: DocumentFile, relativePath: RelativePath): DocumentFile? {
        val parts = relativePath.splitParts()
        var current = rootDoc
        for (part in parts) {
            if (part.isEmpty()) continue
            current = current.findFile(part) ?: return null
        }
        return current
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
    }
}
