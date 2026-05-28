package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.ProjectFile
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class CopyToAssetsInput(
    val project: Project,
    val assetPath: FileSystemPath,
)

class CopyToAssetsUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<CopyToAssetsInput, ProjectFile> {
    override suspend fun invoke(input: CopyToAssetsInput): ProjectFile {
        return projectRepository.copyFromFileSystem(
            project = input.project,
            fromPath = input.assetPath,
            toDirPath = input.project.assetsRelativePath,
        )
    }
}
