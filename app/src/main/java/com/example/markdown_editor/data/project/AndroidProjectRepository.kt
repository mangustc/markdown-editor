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
import androidx.room.RoomRawQuery
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteEntity
import com.example.markdown_editor.data.database.ProjectDao
import com.example.markdown_editor.data.database.ProjectEntity
import com.example.markdown_editor.domain.exceptions.FileNotFoundException
import com.example.markdown_editor.domain.exceptions.FileNotReadableException
import com.example.markdown_editor.domain.exceptions.FileNotWritableException
import com.example.markdown_editor.domain.exceptions.ProjectAccessException
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SearchQuery
import com.example.markdown_editor.domain.repositories.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class AndroidProjectRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override suspend fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): List<Note> = withContext(Dispatchers.IO) {
        val sqlQuery = buildSQLiteQuery(project, query)
        val entities = noteDao.searchNotes(sqlQuery)

        entities.map { entity -> getNoteFromEntity(project, includeText, entity) }
    }

    override fun getNotesPaged(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Flow<PagingData<Note>> {
        val sqlQuery = buildSQLiteQuery(project, query)
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

    override suspend fun getNote(
        project: Project,
        relativePath: RelativePath,
        includeText: Boolean,
        includeFrontMatter: Boolean,
    ): Note = withContext(Dispatchers.IO) {
        val uri = getUri(project.rootFileSystemPath.value.toUri(), relativePath)
        val fullText = readFullText(uri)
        val (frontMatter, text) = FrontMatter.splitFromContent(fullText)
        val documentFile = DocumentFile.fromSingleUri(context, uri)
            ?: throw FileNotFoundException(relativePath.value)
        val name = documentFile.name?.removeSuffix(".md")
            ?: throw FileNotReadableException(relativePath.value)

        Note(
            name = name,
            projectFile = ProjectFile(
                fileSystemPath = FileSystemPath(uri.toString()),
                relativePath = relativePath,
            ),
            lastModified = documentFile.lastModified(),
            createdAt = frontMatter.toCreatedAtMillis(),
            tags = frontMatter.tags,
            body = if (includeText && includeFrontMatter) {
                fullText
            } else if (includeText || includeFrontMatter) buildString {
                if (includeFrontMatter) append(frontMatter.toString())
                if (includeText) append(text)
            } else {
                null
            },
        )
    }

    override suspend fun syncDatabase(project: Project) = withContext(Dispatchers.IO) {
        val projectId = getOrCreateProjectId(project)
            ?: throw ProjectAccessException(project.notesRelativePath.value)

        val projectUri = project.rootFileSystemPath.value.toUri()
        val notesDir =
            DocumentFile.fromTreeUri(context, getUri(projectUri, project.notesRelativePath))
                ?: throw ProjectAccessException(project.notesRelativePath.value)
        val files =
            notesDir.listFiles().filter { it.name?.endsWith(".md") == true }

        val existingNotes = noteDao.searchNotes(buildSQLiteQuery(project, SearchQuery()))
        val existingUris = existingNotes.associateBy { it.uri }

        files.forEach { file ->
            val uriStr = file.uri.toString()
            val cached = existingUris[uriStr]

            if (cached == null || file.lastModified() > cached.lastModified) {
                val fullText = readFullText(file.uri)
                val (frontMatter, body) = FrontMatter.splitFromContent(fullText)
                val tags = frontMatter.toTagString()
                val name = file.name?.removeSuffix(".md")
                    ?: throw FileNotReadableException(uriStr)

                val entity = NoteEntity(
                    id = cached?.id ?: 0,
                    uri = uriStr,
                    name = name,
                    lastModified = file.lastModified(),
                    createdAt = frontMatter.toCreatedAtMillis(),
                    tags = tags,
                    body = body,
                    projectId = projectId,
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
                ?: throw ProjectAccessException(projectPath.value)
            val notesDirPath = Project.DefaultNotesRelativePath.value
            val assetsDirPath = Project.DefaultAssetsRelativePath.value
            val name = root.name
                ?: throw ProjectAccessException(projectPath.value)
            root.findFile(notesDirPath) ?: root.createDirectory(notesDirPath)
            ?: throw ProjectAccessException(notesDirPath)
            root.findFile(assetsDirPath) ?: root.createDirectory(assetsDirPath)
            ?: throw ProjectAccessException(assetsDirPath)
            Project(
                name = name,
                rootFileSystemPath = FileSystemPath(projectPath.toString()),
                notesRelativePath = Project.DefaultNotesRelativePath,
                assetsRelativePath = Project.DefaultAssetsRelativePath,
            )
        }

    override suspend fun copyFromFileSystem(
        project: Project,
        fromPath: FileSystemPath,
        toDirPath: RelativePath,
    ): ProjectFile =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val assetUri = fromPath.value.toUri()
            val sourceFile = DocumentFile.fromSingleUri(context, assetUri)
                ?: throw FileNotReadableException(fromPath.value)
            val fileName = sourceFile.name
                ?: throw FileNotReadableException(fromPath.value)
            val mimeType = resolver.getType(assetUri) ?: "application/octet-stream"

            val rootUri = project.rootFileSystemPath.value.toUri()
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
                ?: throw ProjectAccessException(project.rootFileSystemPath.value)

            val dir = findDocumentFile(rootDoc, toDirPath)
            val targetFile = dir.createFile(mimeType, fileName)
                ?: throw FileNotWritableException(toDirPath.appendRelativePath(RelativePath(fileName)).value)

            try {
                copyFullText(sourceFile.uri, targetFile.uri)
            } catch (e: Exception) {
                targetFile.delete()
                throw e
            }

            ProjectFile(
                fileSystemPath = FileSystemPath(targetFile.uri.toString()),
                relativePath = toDirPath.appendRelativePath(
                    RelativePath(
                        targetFile.name
                            ?: throw FileNotWritableException(targetFile.uri.toString()),
                    ),
                ),
            )
        }

    override suspend fun getAllTags(project: Project): List<String> = withContext(Dispatchers.IO) {
        val projectId = getOrCreateProjectId(project)
            ?: throw ProjectAccessException(project.rootFileSystemPath.value)
        noteDao.getAllTags(projectId).flatMap { it.split(" ") }.filter { it.isNotBlank() }
            .distinct()
    }

    override suspend fun writeFile(
        project: Project,
        relativePath: RelativePath,
        byteArray: ByteArray,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw ProjectAccessException(project.rootFileSystemPath.value)

        val fileName = relativePath.basename
        val dirParts = relativePath.dirRelativePath.splitParts()

        var currentDir = rootDoc
        for (part in dirParts) {
            if (part.isEmpty()) continue
            var nextDir = currentDir.findFile(part)
            if (nextDir == null) {
                if (createParents) {
                    nextDir = currentDir.createDirectory(part)
                        ?: throw FileNotWritableException(relativePath.value)
                } else {
                    throw FileNotFoundException(relativePath.value)
                }
            } else if (!nextDir.isDirectory) {
                throw FileNotWritableException(relativePath.value)
            }
            currentDir = nextDir
        }

        var fileDoc = currentDir.findFile(fileName)
        val shouldCreateFile =
            fileDoc == null || fileExistsStrategy == ProjectRepository.FileExistsStrategy.AUTO_RENAME
        if (!shouldCreateFile) {
            if (fileDoc.isDirectory) {
                throw FileNotWritableException(relativePath.value)
            }
            if (fileExistsStrategy != ProjectRepository.FileExistsStrategy.OVERWRITE) {
                throw FileNotWritableException(relativePath.value)
            }
        } else {
            val extension = fileName.substringAfterLast('.', "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
            fileDoc = currentDir.createFile(mimeType, fileName)
                ?: throw FileNotWritableException(relativePath.value)
        }

        val output = openOutputStream(fileDoc.uri, "wt")
        try {
            output.use { outputStream ->
                outputStream.write(byteArray)
            }
        } catch (e: Exception) {
            throw FileNotWritableException(relativePath.value, e)
        }

        val actualName = fileDoc.name
            ?: throw FileNotWritableException(relativePath.value)
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

        try {
            findDocumentFile(rootDoc, relativePath).delete()
        } catch (_: Exception) {
            return@withContext
        }
    }

    override suspend fun copyFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val bytes = readFile(project, relativePath)
        return@withContext writeFile(
            project,
            newRelativePath,
            bytes,
            fileExistsStrategy,
            createParents,
        )
    }

    override suspend fun moveFile(
        project: Project,
        relativePath: RelativePath,
        newRelativePath: RelativePath,
        fileExistsStrategy: ProjectRepository.FileExistsStrategy,
        createParents: Boolean,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val newProjectFile =
            copyFile(project, relativePath, newRelativePath, fileExistsStrategy, createParents)
        deleteFile(project, relativePath)
        newProjectFile
    }

    override suspend fun readFile(
        project: Project,
        relativePath: RelativePath,
    ): ByteArray = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw ProjectAccessException(project.rootFileSystemPath.value)

        val targetDoc = findDocumentFile(rootDoc, relativePath)

        if (!targetDoc.isFile) {
            throw FileNotReadableException(relativePath.value)
        }

        val input = openInputStream(targetDoc.uri)
        try {
            input.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            throw FileNotReadableException(relativePath.value, e)
        }
    }

    override suspend fun getProjectFilesList(
        project: Project,
    ): List<ProjectFile> = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw ProjectAccessException(project.rootFileSystemPath.value)

        val result = mutableListOf<ProjectFile>()
        walkDocumentTree(rootDoc, RelativePath(""), result)
        result.toList()
    }

    override suspend fun getProjectFile(
        project: Project,
        relativePath: RelativePath,
    ): ProjectFile = withContext(Dispatchers.IO) {
        val rootUri = project.rootFileSystemPath.value.toUri()
        val rootDoc = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw ProjectAccessException(project.rootFileSystemPath.value)
        val targetDoc = findDocumentFile(rootDoc, relativePath)

        return@withContext ProjectFile(
            fileSystemPath = FileSystemPath(
                targetDoc.uri.toString(),
            ),
            relativePath = relativePath,
        )
    }

    private suspend fun getOrCreateProjectId(project: Project): Long? {
        val rootPath = project.rootFileSystemPath.value
        val existingId = projectDao.getProjectId(rootPath)
        if (existingId != null) return existingId

        val newId = projectDao.insertProject(
            ProjectEntity(
                name = project.name,
                rootPath = rootPath,
            ),
        )
        if (newId != -1L) return newId

        return projectDao.getProjectId(rootPath)
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

    private fun findDocumentFile(rootDoc: DocumentFile, relativePath: RelativePath): DocumentFile {
        val parts = relativePath.splitParts()
        var current = rootDoc
        for (part in parts) {
            if (part.isEmpty()) continue
            current = current.findFile(part)
                ?: throw FileNotFoundException(relativePath.value)
        }
        return current
    }

    private fun readFullText(uri: Uri): String {
        val stream = openInputStream(uri)

        return try {
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (e: Exception) {
            throw FileNotReadableException(uri.toString(), e)
        }
    }

    private fun copyFullText(sourceUri: Uri, targetUri: Uri) {
        val input = openInputStream(sourceUri)
        val output = openOutputStream(targetUri)

        try {
            input.use { inputStream ->
                output.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            throw FileNotWritableException(targetUri.toString(), e)
        }
    }

    private fun openInputStream(uri: Uri): InputStream =
        try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            throw FileNotFoundException(uri.toString(), e)
        } ?: throw FileNotFoundException(uri.toString())

    private fun openOutputStream(uri: Uri, mode: String = "w"): OutputStream =
        try {
            context.contentResolver.openOutputStream(uri, mode)
        } catch (e: Exception) {
            throw FileNotFoundException(uri.toString(), e)
        } ?: throw FileNotFoundException(uri.toString())

    private fun buildSQLiteQuery(project: Project, query: SearchQuery): RoomRawQuery {
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
        conditions.add("notes.projectId = (SELECT id FROM projects WHERE rootPath = ? LIMIT 1)")
        args.add(project.rootFileSystemPath.value)
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

        return RoomRawQuery(
            sql = sb.toString(),
            onBindStatement = { statement ->
                args.forEachIndexed { index, arg ->
                    when (arg) {
                        is String -> statement.bindText(index + 1, arg)
                        is Long -> statement.bindLong(index + 1, arg)
                        is Int -> statement.bindLong(index + 1, arg.toLong())
                        is Double -> statement.bindDouble(index + 1, arg)
                        is Boolean -> statement.bindBoolean(index + 1, arg)
                        else -> throw IllegalArgumentException("Unknown argument type")
                    }
                }
            },
        )
    }
}
