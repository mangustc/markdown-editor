package com.example.markdown_editor.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.example.markdown_editor.data.model.Project
import com.example.markdown_editor.data.sync.SyncFileAction.ConflictUpload
import com.example.markdown_editor.data.sync.SyncFileAction.DeleteLocal
import com.example.markdown_editor.data.sync.SyncFileAction.DeleteRemote
import com.example.markdown_editor.data.sync.SyncFileAction.Download
import com.example.markdown_editor.data.sync.SyncFileAction.NoOp
import com.example.markdown_editor.data.sync.SyncFileAction.Upload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.MessageDigest

class SyncEngine(
    private val context: Context,
    private val provider: SyncProvider,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun sync(project: Project): SyncResult = withContext(Dispatchers.IO) {
        val remoteRoot = project.name

        val localManifest = SyncManifest(
            lastSyncTimestamp = System.currentTimeMillis() / 1000,
            files = snapshotLocal(project),
        )
        val baseManifest = runCatching {
            readLocalFile(project, SyncManifest.MANIFEST_PATH)?.let {
                json.decodeFromString<SyncManifest>(it.decodeToString())
            }
        }.getOrNull() ?: SyncManifest.EMPTY
        val remoteManifest = loadRemoteManifest(remoteRoot) ?: SyncManifest.EMPTY

        if (localManifest.files == baseManifest.files && remoteManifest.files == baseManifest.files) {
            return@withContext SyncResult(actions = emptyList(), newManifest = baseManifest)
        }

        val allPaths =
            (localManifest.files.keys + remoteManifest.files.keys + baseManifest.files.keys).toSet()
        val actions = allPaths.map { path ->
            decide(
                path,
                localManifest.files[path],
                remoteManifest.files[path],
                baseManifest.files[path],
            )
        }

        val errors = mutableListOf<String>()
        actions.forEach { action ->
            try {
                execute(action, project, remoteRoot)
            } catch (e: Exception) {
                errors.add("${action::class.simpleName}(${actionPath(action)}): ${e.message}")
            }
        }

        val finalLocalState = snapshotLocal(project)
        val manifest = SyncManifest(
            lastSyncTimestamp = System.currentTimeMillis() / 1000,
            files = finalLocalState,
        )
        saveManifest(project, remoteRoot, manifest)

        SyncResult(actions = actions, newManifest = manifest, errors = errors)
    }

    private fun decide(
        path: String,
        l: String?,
        r: String?,
        b: String?,
    ): SyncFileAction = when {
        l != null && r == null && b == null -> Upload(path)
        l == null && r != null && b == null -> Download(path)
        l != null && l != b && r != null && r == b -> Upload(path)
        l != null && l == b && r != null && r != b -> Download(path)
        l != null && r == null && b != null -> DeleteLocal(path)
        l == null && r != null && b != null -> DeleteRemote(path)
        l != null && l != b && r != null && r != b -> ConflictUpload(path)
        else -> NoOp(path)
    }

    private suspend fun execute(action: SyncFileAction, project: Project, remoteRoot: String) {
        val remotePath = "$remoteRoot/${actionPath(action)}"
        when (action) {
            is Upload, is ConflictUpload -> {
                val bytes = readLocalFile(project, actionPath(action))
                    ?: throw IllegalStateException("Local file missing for upload")
                provider.uploadFile(remotePath, bytes)
            }

            is Download -> {
                val bytes = provider.downloadFile(remotePath)
                    ?: throw IllegalStateException("Remote file missing for download")
                writeLocalFile(project, actionPath(action), bytes)
            }

            is DeleteLocal -> deleteLocalFile(project, actionPath(action))
            is DeleteRemote -> provider.deleteFile(remotePath)
            is NoOp -> Unit
        }
    }

    private fun snapshotLocal(project: Project): Map<String, String> {
        val root = DocumentFile.fromTreeUri(context, project.uri) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        walkDocumentTree(root, "", result)
        result.remove(SyncManifest.MANIFEST_PATH)
        return result
    }

    private fun walkDocumentTree(
        dir: DocumentFile,
        prefix: String,
        out: MutableMap<String, String>,
    ) {
        dir.listFiles().forEach { file ->
            val name = file.name ?: return@forEach
            val relPath = if (prefix.isEmpty()) name else "$prefix/$name"
            if (file.isDirectory) {
                walkDocumentTree(file, relPath, out)
            } else {
                val bytes = context.contentResolver.openInputStream(file.uri)
                    ?.use { it.readBytes() } ?: return@forEach
                out[relPath] = md5(bytes)
            }
        }
    }

    private fun readLocalFile(project: Project, relativePath: String): ByteArray? {
        val uri = project.getFileUri(relativePath)
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun writeLocalFile(project: Project, relativePath: String, bytes: ByteArray) {
        val parts = relativePath.split("/")
        val root = DocumentFile.fromTreeUri(context, project.uri)
            ?: throw IllegalStateException("Cannot access project root")

        var dir = root
        for (segment in parts.dropLast(1)) {
            dir = dir.findFile(segment) ?: dir.createDirectory(segment)
                    ?: throw IllegalStateException("Cannot create directory $segment")
        }

        val fileName = parts.last()
        val existing = dir.findFile(fileName)
        val targetUri = if (existing != null) {
            existing.uri
        } else {
            val mime = if (fileName.endsWith(".md")) "text/markdown" else "application/octet-stream"
            dir.createFile(mime, fileName)?.uri
                ?: throw IllegalStateException("Cannot create file $fileName")
        }

        context.contentResolver.openOutputStream(targetUri, "wt")
            ?.use { it.write(bytes) }
    }

    private fun deleteLocalFile(project: Project, relativePath: String) {
        val uri = project.getFileUri(relativePath)
        val file = DocumentFile.fromSingleUri(context, uri)
        if (file?.exists() == true) file.delete()
    }

    private suspend fun loadRemoteManifest(remoteRoot: String): SyncManifest? {
        val bytes = provider.downloadFile("$remoteRoot/${SyncManifest.MANIFEST_PATH}")
            ?: return null
        return runCatching { json.decodeFromString<SyncManifest>(bytes.decodeToString()) }
            .getOrNull()
    }

    private suspend fun saveManifest(
        project: Project,
        remoteRoot: String,
        manifest: SyncManifest,
    ) {
        val encoded = json.encodeToString(manifest).toByteArray()
        provider.uploadFile("$remoteRoot/${SyncManifest.MANIFEST_PATH}", encoded)
        writeLocalFile(project, SyncManifest.MANIFEST_PATH, encoded)
    }

    private fun md5(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun actionPath(action: SyncFileAction): String = when (action) {
        is Upload -> action.relativePath
        is Download -> action.relativePath
        is DeleteLocal -> action.relativePath
        is DeleteRemote -> action.relativePath
        is ConflictUpload -> action.relativePath
        is NoOp -> action.relativePath
    }
}

data class SyncResult(
    val actions: List<SyncFileAction>,
    val newManifest: SyncManifest,
    val errors: List<String> = emptyList(),
) {
    val hasErrors get() = errors.isNotEmpty()
}