package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.data.database.NoteDao
import com.example.markdown_editor.data.database.NoteEntity
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProjectRepositoryImpl(
    private val context: Context,
    private val noteDao: NoteDao,
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "project_prefs",
        Context.MODE_PRIVATE
    )
) : ProjectRepository {
    override suspend fun syncDatabase(project: Project) = withContext(Dispatchers.IO) {
        val notesDir = DocumentFile.fromTreeUri(context, project.notesUri)
        val files = notesDir?.listFiles()?.filter { it.name?.endsWith(".md") == true } ?: return@withContext

        val existingNotes = noteDao.searchMetadata("", "")
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
            if (!currentFileUris.contains(cached.uri)) {
                noteDao.deleteByUri(cached.uri)
            }
        }
    }

    override suspend fun saveProject(project: Project) {
        // Persist the URI so it survives app restart
        // takePersistableUriPermission ensures we can access it after reboot
        context.contentResolver.takePersistableUriPermission(
            project.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        prefs.edit()
            .putString(KEY_PROJECT_URI, project.uri.toString())
            .putString(KEY_PROJECT_NAME, project.name)
            .apply()
    }

    override suspend fun loadSavedProject(): Project? = withContext(Dispatchers.IO) {
        val uriString = prefs.getString(KEY_PROJECT_URI, null) ?: return@withContext null
        val name = prefs.getString(KEY_PROJECT_NAME, null) ?: return@withContext null
        val uri = uriString.toUri()
        buildProject(uri, name)
    }

    override suspend fun createNote(project: Project, name: String?, tags: List<String>?): Uri? =
        withContext(Dispatchers.IO) {
            val notesDir =
                DocumentFile.fromTreeUri(context, project.notesUri) ?: return@withContext null

            val isoDate = java.time.Instant.now().toString()
            var frontMatterBuilder = """---
createdAt: $isoDate
""".trimIndent()
            if (!tags.isNullOrEmpty()) {
                frontMatterBuilder += "\ntags:"
                tags.forEach { tag ->
                    frontMatterBuilder += "\n- $tag"
                }
            }
            val initialContent = "$frontMatterBuilder\n---".trimIndent()

            val newFile =
                notesDir.createFile("text/markdown", "$name.md") ?: return@withContext null
            context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write(initialContent)
                }
            }

            newFile.uri
        }

    // Derives notes/ and assets/ URIs from the root project URI
    override fun buildProject(rootUri: Uri, name: String): Project {
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

    override suspend fun getNotes(
        project: Project,
        query: SearchQuery,
        includeText: Boolean,
        includeFrontMatter: Boolean
    ): List<Note> =
        withContext(Dispatchers.IO) {
            val entities = if (query.bodyTerms.isNotEmpty()) {
                val ftsQuery = query.bodyTerms.joinToString(" AND ") { "$it*" }
                noteDao.searchFullText(ftsQuery)
            } else {
                noteDao.searchMetadata(
                    tag = query.tagFilters.firstOrNull() ?: "",
                    name = query.nameFilter ?: ""
                )
            }
            return@withContext entities.map { Note(
                name = it.name,
                uri = it.uri.toUri(),
                lastModified = it.lastModified,
                createdAt = it.createdAt,
                text = if (includeText) it.body else null,
            ) }
        }

    override suspend fun getNoteText(note: Note, includeFrontMatter: Boolean): String =
        withContext(Dispatchers.IO) {
            val fullText = context.contentResolver
                .openInputStream(note.uri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: ""
            return@withContext if (includeFrontMatter) fullText else splitFrontMatter(fullText).second
        }

    override suspend fun saveNoteText(note: Note, text: String): Note =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(note.uri, "wt")?.bufferedWriter()
                ?.use { writer ->
                    writer.write(text)
                }
            note
        }

    override suspend fun getNoteByUri(uri: Uri): Note = withContext(Dispatchers.IO) {
        val content = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: ""
        } catch (e: Exception) {
            // Handle read failure gracefully
            return@withContext Note(
                name = "Error",
                uri = uri,
                lastModified = System.currentTimeMillis(),
                createdAt = null
            )
        }

        val (frontMatterString, _) = splitFrontMatter(content)
        val frontMatter = parseFrontMatter(frontMatterString)
        val createdAtLong = getCreatedAt(frontMatter)
        val documentFile = DocumentFile.fromSingleUri(context, uri) ?: return@withContext Note(
            name = "Unknown",
            uri = uri,
            lastModified = 0L,
            createdAt = null
        )

        Note(
            name = documentFile.name?.removeSuffix(".md") ?: "Untitled",
            uri = uri,
            lastModified = documentFile.lastModified(),
            createdAt = createdAtLong
        )
    }

    override suspend fun copyToAssets(project: Project, assetUri: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        val sourceFile = DocumentFile.fromSingleUri(context, assetUri)
        val fileName = sourceFile?.name ?: "attachment_${System.currentTimeMillis()}"
        val mimeType = resolver.getType(assetUri) ?: "application/octet-stream"

        val assetsDir = DocumentFile.fromTreeUri(context, project.assetsUri)
            ?: throw IllegalStateException("Could not access assets directory")
        val targetFile = assetsDir.createFile(mimeType, fileName)
            ?: throw IllegalStateException("Failed to create file in assets")

        try {
            resolver.openInputStream(assetUri)?.use { inputStream ->
                resolver.openOutputStream(targetFile.uri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            targetFile.delete()
            throw e
        }

        "assets/${targetFile.name}"
    }
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
                i += 1
                continue
            }

            val kv = line.split(":", limit = 2).map { it.trim() }
            if (kv.size != 2) {
                throw IllegalArgumentException("Invalid line format: $line")
            }

            val key = kv[0]
            val valuePart = kv[1]

            if (valuePart.isEmpty()) {
                val list = mutableListOf<String>()
                i += 1
                while (i < lines.size && lines[i].trimStart().startsWith("-")) {
                    val item = lines[i].trim().removePrefix("-").trim()
                    list.add(item)
                    i += 1
                }
                result[key] = list.toList()
            } else {
                result[key] = valuePart
                i += 1
            }
        }

        return result
    }

    private fun getCreatedAt(frontMatter: Map<String, Any>): Long? {
        return frontMatter["createdAt"]?.let { value ->
            try {
                java.time.Instant.parse(value as String).toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun readFullText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""

    companion object {
        private const val KEY_PROJECT_URI = "project_uri"
        private const val KEY_PROJECT_NAME = "project_name"
    }
}