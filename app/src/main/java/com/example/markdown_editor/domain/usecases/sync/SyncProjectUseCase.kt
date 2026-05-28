package com.example.markdown_editor.domain.usecases.sync

import com.example.markdown_editor.data.project.ProjectRepository
import com.example.markdown_editor.data.sync.SyncRepository
import com.example.markdown_editor.data.sync.SyncRepositoryFactory
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.usecases.UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.security.MessageDigest

data class SyncProjectInput(
    val project: Project,
    val settings: Settings,
)

class SyncProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val syncRepositoryFactory: SyncRepositoryFactory,
) : UseCase<SyncProjectInput, Unit> {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun invoke(input: SyncProjectInput) {
        val syncRepository = syncRepositoryFactory.create(input.settings) ?: return
        sync(input.project, syncRepository)
    }

    private suspend fun sync(
        project: Project,
        syncRepository: SyncRepository,
    ) = withContext(Dispatchers.IO) {
        val remoteRoot = RelativePath(project.name)

        val localManifest = SyncManifest(
            lastSyncTimestamp = System.currentTimeMillis() / 1000,
            files = snapshotLocal(project),
        )

        val baseManifest = runCatching {
            projectRepository.readFile(project, SyncManifest.ProjectRelativePath)?.let {
                json.decodeFromString<SyncManifest>(it.decodeToString())
            }
        }.getOrNull() ?: SyncManifest.Empty

        val remoteManifest =
            syncRepository.downloadFile(remoteRoot.appendRelativePath(SyncManifest.ProjectRelativePath))
                ?.let {
                    runCatching {
                        json.decodeFromString<SyncManifest>(it.decodeToString())
                    }.getOrNull()
                } ?: SyncManifest.Empty

        if (localManifest.files == baseManifest.files && remoteManifest.files == baseManifest.files) {
            return@withContext
        }

        val allPaths =
            (localManifest.files.keys + remoteManifest.files.keys + baseManifest.files.keys).toSet()
        val actions = allPaths.map { path ->
            val relativePath = RelativePath(path)
            decide(
                relativePath,
                localManifest.files[path],
                remoteManifest.files[path],
                baseManifest.files[path],
            )
        }

        actions.forEach { action ->
            val remotePath = remoteRoot.appendRelativePath(actionPath(action))
            when (action) {
                is SyncFileAction.Upload, is SyncFileAction.ConflictUpload -> {
                    val bytes = projectRepository.readFile(project, actionPath(action))
                        ?: throw SyncLocalIoException()
                    syncRepository.uploadFile(remotePath, bytes)
                }

                is SyncFileAction.Download -> {
                    val bytes = syncRepository.downloadFile(remotePath)
                        ?: throw SyncStateException()
                    projectRepository.writeFile(project, actionPath(action), bytes)
                }

                is SyncFileAction.DeleteLocal -> projectRepository.deleteFile(
                    project,
                    actionPath(action),
                )

                is SyncFileAction.DeleteRemote -> syncRepository.deleteFile(remotePath)
                is SyncFileAction.NoOp -> Unit
            }
        }


        val finalLocalState = snapshotLocal(project)
        val manifest = SyncManifest(
            lastSyncTimestamp = System.currentTimeMillis() / 1000,
            files = finalLocalState,
        )

        val newManifestBytes = json.encodeToString(manifest).toByteArray()
        syncRepository.uploadFile(
            remoteRoot.appendRelativePath(SyncManifest.ProjectRelativePath),
            newManifestBytes,
        )
        projectRepository.writeFile(project, SyncManifest.ProjectRelativePath, newManifestBytes)
    }

    private fun decide(
        path: RelativePath,
        l: String?,
        r: String?,
        b: String?,
    ): SyncFileAction = when {
        l != null && r == null && b == null -> SyncFileAction.Upload(path)
        l == null && r != null && b == null -> SyncFileAction.Download(path)
        l != null && l != b && r != null && r == b -> SyncFileAction.Upload(path)
        l != null && l == b && r != null && r != b -> SyncFileAction.Download(path)
        l != null && r == null && b != null -> SyncFileAction.DeleteLocal(path)
        l == null && r != null && b != null -> SyncFileAction.DeleteRemote(path)
        l != null && l != b && r != null && r != b -> SyncFileAction.ConflictUpload(path)
        else -> SyncFileAction.NoOp(path)
    }

    private suspend fun snapshotLocal(
        project: Project,
    ): Map<String, String> {
        return projectRepository.getProjectFilesList(project).mapNotNull { file ->
            if (file.relativePath == SyncManifest.ProjectRelativePath) {
                null
            } else {
                val bytes = projectRepository.readFile(project, file.relativePath)
                if (bytes == null) {
                    null
                } else {
                    file.relativePath.toString() to md5(bytes)
                }
            }
        }.toMap()
    }

    private fun md5(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun actionPath(action: SyncFileAction): RelativePath = when (action) {
        is SyncFileAction.Upload -> action.relativePath
        is SyncFileAction.Download -> action.relativePath
        is SyncFileAction.DeleteLocal -> action.relativePath
        is SyncFileAction.DeleteRemote -> action.relativePath
        is SyncFileAction.ConflictUpload -> action.relativePath
        is SyncFileAction.NoOp -> action.relativePath
    }
}