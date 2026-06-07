package com.example.markdown_editor.domain.usecases.sync

import com.example.markdown_editor.data.sync.SyncRepositoryFactory
import com.example.markdown_editor.domain.exceptions.SyncStateException
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.repositories.SyncRepository
import com.example.markdown_editor.domain.usecases.UseCase
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.MD5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock

data class SyncProjectInput(
    val project: Project,
    val settings: Settings,
)

class SyncProjectUseCase(
    private val projectRepository: ProjectRepository,
    private val syncRepositoryFactory: SyncRepositoryFactory,
) : UseCase<SyncProjectInput, Unit> {
    @OptIn(DelicateCryptographyApi::class)
    private val md5Provider = CryptographyProvider.Default.get(MD5).hasher()
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
            lastSyncTimestamp = Clock.System.now().epochSeconds,
            files = snapshotLocal(project),
        )

        val baseManifest = runCatching {
            projectRepository.readFile(project, SyncManifest.ProjectRelativePath).let {
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
            val remotePath = remoteRoot.appendRelativePath(action.relativePath)
            when (action) {
                is SyncFileAction.Upload, is SyncFileAction.ConflictUpload -> {
                    val bytes = projectRepository.readFile(project, action.relativePath)
                    syncRepository.uploadFile(remotePath, bytes)
                }

                is SyncFileAction.Download -> {
                    val bytes = syncRepository.downloadFile(remotePath)
                        ?: throw SyncStateException()
                    projectRepository.writeFile(
                        project = project,
                        relativePath = action.relativePath,
                        byteArray = bytes,
                        fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
                    )
                }

                is SyncFileAction.DeleteLocal -> projectRepository.deleteFile(
                    project = project,
                    relativePath = action.relativePath,
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
        projectRepository.writeFile(
            project = project,
            relativePath = SyncManifest.ProjectRelativePath,
            byteArray = newManifestBytes,
            fileExistsStrategy = ProjectRepository.FileExistsStrategy.OVERWRITE,
        )
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
                file.relativePath.toString() to md5(bytes)
            }
        }.toMap()
    }

    private suspend fun md5(bytes: ByteArray): String {
        return md5Provider.hash(bytes).joinToString("") { "%02x".format(it) }
    }
}