package com.example.markdown_editor.domain.usecases.project

import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SpanInfo
import com.example.markdown_editor.domain.repositories.ProjectRepository
import com.example.markdown_editor.domain.usecases.UseCase

data class GetRealSpanInfoLinkTypeInput(
    val project: Project,
    val span: SpanInfo.Link,
)

class GetRealSpanInfoLinkTypeUseCase(
    private val projectRepository: ProjectRepository,
) : UseCase<GetRealSpanInfoLinkTypeInput, SpanInfo.Link.LinkType?> {
    override suspend fun invoke(input: GetRealSpanInfoLinkTypeInput): SpanInfo.Link.LinkType? {
        when (val linkType = input.span.linkType) {
            SpanInfo.Link.LinkType.NOTE, SpanInfo.Link.LinkType.FILE -> {
                val projectFile = projectRepository.getProjectFile(
                    input.project,
                    RelativePath(input.span.payload),
                ) ?: return null
                val isNote =
                    linkType == SpanInfo.Link.LinkType.NOTE &&
                            projectFile.relativePath.dirRelativePath == input.project.notesRelativePath

                return if (isNote) {
                    SpanInfo.Link.LinkType.NOTE
                } else {
                    SpanInfo.Link.LinkType.FILE
                }
            }

            SpanInfo.Link.LinkType.HTTP -> {
                return SpanInfo.Link.LinkType.HTTP
            }
        }
    }
}
