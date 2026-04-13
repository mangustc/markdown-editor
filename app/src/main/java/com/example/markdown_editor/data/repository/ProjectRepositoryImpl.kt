package com.example.markdown_editor.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.model.SearchQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class ProjectRepositoryImpl(
    private val context: Context,
    private val prefs: SharedPreferences = context.getSharedPreferences("project_prefs", Context.MODE_PRIVATE)
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

    override suspend fun createNote(project: Project, name: String?, tags: List<String>?): Uri? = withContext(Dispatchers.IO) {
        val notesDir = DocumentFile.fromTreeUri(context, project.notesUri) ?: return@withContext null

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

        val newFile = notesDir.createFile("text/markdown", "$name.md") ?: return@withContext null
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

    override suspend fun searchNotes(project: Project, query: SearchQuery): List<Note> =
        withContext(Dispatchers.IO) {
            if (query.isEmpty) return@withContext emptyList()

            val notesDir = DocumentFile.fromTreeUri(context, project.notesUri)
            val files = notesDir?.listFiles()
                ?.filter { it.name?.endsWith(".md") == true }
                ?: return@withContext emptyList()

            files.mapNotNull { file ->
                val note = Note(
                    name = file.name?.removeSuffix(".md") ?: "Untitled",
                    uri  = file.uri,
                    lastModified = file.lastModified()
                )

                query.nameFilter?.let { nameFilter ->
                    if (!note.name.contains(nameFilter, ignoreCase = true)) return@mapNotNull null
                }

                if (query.bodyTerms.isEmpty() && query.tagFilters.isEmpty() && query.propFilters.isEmpty()) {
                    return@mapNotNull note
                }

                val content = try {
                    context.contentResolver.openInputStream(file.uri)
                        ?.bufferedReader()?.readText() ?: ""
                } catch (e: Exception) { "" }

                val (frontMatterString, body) = splitFrontMatter(content)
                val frontMatter = parseFrontMatter(frontMatterString)

                if (query.tagFilters.isNotEmpty()) {
                    val fileTags = when (val tags = frontMatter["tags"]) {
                        is List<*> -> tags.filterIsInstance<String>()
                        else -> emptyList()
                    }
                    if (!query.tagFilters.all { t -> fileTags.any { it.contains(t, ignoreCase = true) } })
                        return@mapNotNull null
                }

                for ((key, value) in query.propFilters) {
                    val fmValue = frontMatter[key] as? String ?: return@mapNotNull null
                    if (!fmValue.contains(value, ignoreCase = true)) return@mapNotNull null
                }

                val searchableText = (body + " " + note.name).lowercase()
                if (!query.bodyTerms.all { term -> searchableText.contains(term.lowercase()) })
                    return@mapNotNull null

                note
            }.sortedByDescending { it.lastModified }
        }

    override suspend fun getNoteText(note: Note, includeFrontMatter: Boolean): String = withContext(Dispatchers.IO) {
        val fullText = context.contentResolver
            .openInputStream(note.uri)
            ?.bufferedReader()
            ?.use { it.readText() } ?: ""
        return@withContext if (includeFrontMatter) fullText else splitFrontMatter(fullText).second
    }

    override suspend fun saveNoteText(note: Note, text: String): Note = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(note.uri, "wt")?.bufferedWriter()?.use { writer ->
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

        // Extract creation time from front matter
        val createdAtLong = frontMatter["createdAt"]?.let { value ->
            try {
                // Attempt to parse ISO date string back to Long timestamp
                java.time.Instant.parse(value as String).toEpochMilli()
            } catch (e: Exception) {
                null // Parsing failed
            }
        }

        // Get basic file info for name and lastModified
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

    companion object {
        private const val KEY_PROJECT_URI = "project_uri"
        private const val KEY_PROJECT_NAME = "project_name"
    }
}