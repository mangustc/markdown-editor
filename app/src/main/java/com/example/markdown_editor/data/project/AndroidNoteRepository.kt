package com.example.markdown_editor.data.project

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.FrontMatter
import com.example.markdown_editor.domain.models.Note
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.models.RelativePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

class AndroidNoteRepository(
    private val context: Context,
) : NoteRepository {
    override suspend fun toggleNotePin(note: Note): String = withContext(Dispatchers.IO) {
        val noteUri = note.projectFile.fileSystemPath.value.toUri()
        val fullText = readFullText(noteUri)
        val (frontMatter, body) = FrontMatter.splitFromContent(fullText)

        val currentTags = frontMatter.tags.toMutableList()
        if ("pinned" in currentTags) currentTags.remove("pinned") else currentTags.add("pinned")

        val updatedFields = frontMatter.fields.toMutableMap().apply {
            put("tags", FrontMatter.FrontMatterValue.StringList(currentTags))
        }
        val updatedFrontMatter = frontMatter.copy(fields = updatedFields)
        val newFrontMatterString = updatedFrontMatter.toString()

        context.contentResolver.openOutputStream(noteUri, "wt")
            ?.bufferedWriter()
            ?.use { it.write("$newFrontMatterString\n$body") }

        newFrontMatterString
    }

    override suspend fun createNote(
        project: Project,
        name: String?,
        tags: List<String>?,
    ): FileSystemPath? =
        withContext(Dispatchers.IO) {
            val notesUri =
                getUri(project.rootFileSystemPath.value.toUri(), project.notesRelativePath)
            val notesDir =
                DocumentFile.fromTreeUri(context, notesUri) ?: return@withContext null

            val isoDate = Instant.now().toString()
            var frontMatterBuilder = "---\ncreatedAt: $isoDate"
            frontMatterBuilder += "\ntags:"
            if (!tags.isNullOrEmpty()) {
                tags.forEach { tag -> frontMatterBuilder += "\n- $tag" }
            }
            val initialContent = "$frontMatterBuilder\n---"

            val newFile =
                notesDir.createFile("text/markdown", "$name.md") ?: return@withContext null
            context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { out ->
                out.bufferedWriter().use { it.write(initialContent) }
            }
            FileSystemPath(newFile.uri.toString())
        }

    override suspend fun getNoteText(note: Note, includeFrontMatter: Boolean): String =
        withContext(Dispatchers.IO) {
            val fullText = readFullText(note.projectFile.fileSystemPath.value.toUri())
            if (includeFrontMatter) fullText else FrontMatter.splitFromContent(fullText).second
        }

    override suspend fun saveNoteText(note: Note, text: String): Note =
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(
                note.projectFile.fileSystemPath.value.toUri(),
                "wt",
            )
                ?.bufferedWriter()
                ?.use { it.write(text) }
            note
        }

    override suspend fun getNoteByFileSystemPath(path: FileSystemPath): Note =
        withContext(Dispatchers.IO) {
            val uri = path.value.toUri()
            val (frontMatter, _) = FrontMatter.splitFromContent(readFullText(uri))
            val documentFile = DocumentFile.fromSingleUri(context, uri)
                ?: throw Exception()
            val name = documentFile.name?.removeSuffix(".md") ?: "Untitled"

            Note(
                name = name,
                projectFile = ProjectFile(
                    fileSystemPath = FileSystemPath(uri.toString()),
                    relativePath = RelativePath("notes/${name}"),
                ),
                lastModified = documentFile.lastModified(),
                createdAt = frontMatter.toCreatedAtMillis(),
            )
        }

    override suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        val file =
            DocumentFile.fromSingleUri(context, note.projectFile.fileSystemPath.value.toUri())
        if (file?.exists() == true) file.delete()
    }

    override suspend fun renameNote(note: Note, newName: String) = withContext(Dispatchers.IO) {
        DocumentsContract.renameDocument(
            context.contentResolver,
            note.projectFile.fileSystemPath.value.toUri(),
            "$newName.md",
        ) ?: throw IllegalStateException("Failed to rename file in storage")
        Unit
    }

    private fun getUri(
        rootUri: Uri,
        relativePath: RelativePath,
    ): Uri {
        val treeId = DocumentsContract.getTreeDocumentId(rootUri)
        val childId = if (relativePath.value.isEmpty()) treeId else "$treeId/${relativePath.value}"
        return DocumentsContract.buildDocumentUriUsingTree(rootUri, childId)
    }

    private fun readFullText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
}
