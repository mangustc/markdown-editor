package com.example.markdown_editor.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.data.model.FrontMatter
import com.example.markdown_editor.data.model.FrontMatterValue
import com.example.markdown_editor.data.model.Note
import com.example.markdown_editor.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class NoteRepositoryImpl(
    private val context: Context,
) : NoteRepository {
    override suspend fun toggleNotePin(note: Note): String = withContext(Dispatchers.IO) {
        val fullText = readFullText(note.uri)
        val (frontMatter, body) = FrontMatter.splitFromContent(fullText)

        val currentTags = frontMatter.tags.toMutableList()
        if ("pinned" in currentTags) currentTags.remove("pinned") else currentTags.add("pinned")

        val updatedFields = frontMatter.fields.toMutableMap().apply {
            put("tags", FrontMatterValue.StringList(currentTags))
        }
        val updatedFrontMatter = frontMatter.copy(fields = updatedFields)
        val newFrontMatterString = updatedFrontMatter.toString()

        context.contentResolver.openOutputStream(note.uri, "wt")
            ?.bufferedWriter()
            ?.use { it.write("$newFrontMatterString\n$body") }

        newFrontMatterString
    }

    override suspend fun createNote(project: Project, name: String?, tags: List<String>?): Uri? =
        withContext(Dispatchers.IO) {
            val notesDir =
                DocumentFile.fromTreeUri(context, project.notesUri) ?: return@withContext null

            val isoDate = Instant.now().toString()
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
            val fullText = readFullText(note.uri)
            if (includeFrontMatter) fullText else FrontMatter.splitFromContent(fullText).second
        }

    override suspend fun saveNoteText(note: Note, text: String): Note =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(note.uri, "wt")
                ?.bufferedWriter()
                ?.use { it.write(text) }
            note
        }

    override suspend fun getNoteByUri(uri: Uri): Note = withContext(Dispatchers.IO) {
        val (frontMatter, _) = FrontMatter.splitFromContent(readFullText(uri))
        val documentFile = DocumentFile.fromSingleUri(context, uri)
            ?: throw Exception()

        Note(
            name = documentFile.name?.removeSuffix(".md") ?: "Untitled",
            uri = uri,
            lastModified = documentFile.lastModified(),
            createdAt = frontMatter.toCreatedAtMillis(),
        )
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

    private fun readFullText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
}
