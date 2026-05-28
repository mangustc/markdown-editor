package com.example.markdown_editor.data.project

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
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
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidProjectRepository(
    private val context: Context,
    private val noteDao: NoteDao,
) : ProjectRepository {
    override suspend fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): List<Note> = withContext(Dispatchers.IO) {
        val sqlQuery = buildSQLiteQuery(query)
        val entities = noteDao.searchNotes(sqlQuery)

        entities.map { entity -> getNoteFromEntity(project, includeText, entity) }
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
            pagingData.map { entity -> getNoteFromEntity(project, includeText, entity) }
        }
    }

    override suspend fun getNoteDatabase(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean,
    ): Note? = withContext(Dispatchers.IO) {
        val noteUri = getUri(project.rootFileSystemPath.value.toUri(), relativePath)
        noteDao.getNoteByUri(noteUri.toString())
            ?.let { entity -> getNoteFromEntity(project, includeText, entity) }
    }

    override suspend fun getNote(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Note? = withContext(Dispatchers.IO) {
        val uri = getUri(project.rootFileSystemPath.value.toUri(), relativePath)
        val (frontMatter, text) = FrontMatter.splitFromContent(readFullText(uri))
        val documentFile = DocumentFile.fromSingleUri(context, uri)
            ?: return@withContext null
        val name = documentFile.name?.removeSuffix(".md") ?: return@withContext null

        Note(
            name = name,
            projectFile = ProjectFile(
                fileSystemPath = FileSystemPath(uri.toString()),
                relativePath = relativePath,
            ),
            lastModified = documentFile.lastModified(),
            createdAt = frontMatter.toCreatedAtMillis(),
            tags = frontMatter.tags,
            body = if (includeText || includeFrontMatter) {
                var body = ""
                if (includeFrontMatter) body += frontMatter.toString()
                if (includeText) body += text
                body
            } else {
                null
            },
        )
    }

    override suspend fun syncDatabase(project: Project) = withContext(Dispatchers.IO) {
        val projectUri = project.rootFileSystemPath.value.toUri()
        val notesDir =
            DocumentFile.fromTreeUri(context, getUri(projectUri, project.notesRelativePath))
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

    override suspend fun buildProject(projectPath: FileSystemPath): Project =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, projectPath.value.toUri())
            val notesDir = root?.findFile("notes") ?: root?.createDirectory("notes")
            val assetsDir = root?.findFile("assets") ?: root?.createDirectory("assets")
            Project(
                name = root?.name ?: "Project",
                rootFileSystemPath = FileSystemPath(projectPath.toString()),
                notesRelativePath = RelativePath(if (notesDir != null) "notes" else ""),
                assetsRelativePath = RelativePath(if (assetsDir != null) "assets" else ""),
            )
        }

    override suspend fun copyToAssets(project: Project, assetPath: FileSystemPath): ProjectFile =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val assetUri = assetPath.value.toUri()
            val sourceFile = DocumentFile.fromSingleUri(context, assetUri)
            val fileName = sourceFile?.name ?: "attachment_${System.currentTimeMillis()}"
            val mimeType = resolver.getType(assetUri) ?: "application/octet-stream"
            val projectUri = project.rootFileSystemPath.value.toUri()

            val assetsDir =
                DocumentFile.fromTreeUri(context, getUri(projectUri, project.assetsRelativePath))
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

            ProjectFile(
                fileSystemPath = FileSystemPath(targetFile.uri.toString()),
                relativePath = RelativePath("assets/${targetFile.name}"),
            )
        }

    override suspend fun getAllTags(): List<String> = withContext(Dispatchers.IO) {
        noteDao.getAllTags().flatMap { it.split(" ") }.filter { it.isNotBlank() }.distinct()
    }

    override suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
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
        val shouldCreateFile =
            fileDoc == null || fileExistsStrategy == ProjectRepository.FileExistsStrategy.AUTO_RENAME
        if (!shouldCreateFile) {
            if (fileDoc.isDirectory) {
                return@withContext null
            }
            if (fileExistsStrategy != ProjectRepository.FileExistsStrategy.OVERWRITE) {
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

        val actualName = fileDoc.name ?: return@withContext null
        ProjectFile(
            fileSystemPath = FileSystemPath(fileDoc.uri.toString()),
            relativePath = relativePath.dirRelativePath.appendRelativePath(RelativePath(actualName)),
        )
    }

    override suspend fun deleteFile(
        project: Project,
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
        project: Project,
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
        project: Project,
    ): List<ProjectFile> = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()

        val result = mutableListOf<ProjectFile>()
        walkDocumentTree(rootDoc, RelativePath(""), result)
        return@withContext result.toList()
    }

    override suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile? = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext null
        val targetDoc = findDocumentFile(rootDoc, relativePath) ?: return@withContext null

        return@withContext ProjectFile(
            fileSystemPath = FileSystemPath(
                targetDoc.uri.toString(),
            ),
            relativePath = relativePath,
        )
    }

    private fun getNoteFromEntity(
        project: Project,
        includeText: Boolean,
        entity: NoteEntity,
    ): Note {
        return Note(
            name = entity.name,
            projectFile = ProjectFile(
                fileSystemPath = FileSystemPath(entity.uri),
                relativePath = project.notesRelativePath.appendRelativePath(
                    RelativePath("${entity.name}.md"),
                ),
            ),
            lastModified = entity.lastModified,
            createdAt = entity.createdAt,
            body = if (includeText) entity.body else null,
            tags = if (entity.tags.isNotEmpty()) entity.tags.split(" ") else emptyList(),
        )
    }

    private fun getUri(
        rootUri: Uri,
        relativePath: RelativePath,
    ): Uri {
        val treeId = DocumentsContract.getTreeDocumentId(rootUri)
        val childId = if (relativePath.value.isEmpty()) treeId else "$treeId/${relativePath.value}"
        return DocumentsContract.buildDocumentUriUsingTree(rootUri, childId)
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
            SearchQuery.SortBy.LAST_MODIFIED -> "notes.lastModified DESC"
            SearchQuery.SortBy.CREATED_AT ->
                "notes.createdAt DESC, notes.lastModified DESC"
        }
        sb.append("\nORDER BY $pinnedClause$sortClause")

        return SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
    }
}
