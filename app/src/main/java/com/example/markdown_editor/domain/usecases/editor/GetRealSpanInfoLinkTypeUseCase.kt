package com.example.markdown_editor.domain.usecases.editor

import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.models.SpanInfo
import com.example.markdown_editor.domain.usecases.UseCase
import com.example.markdown_editor.domain.usecases.project.GetProjectFileInput
import com.example.markdown_editor.domain.usecases.project.GetProjectFileUseCase

data class GetRealSpanInfoLinkTypeInput(
    val project: Project,
    val span: SpanInfo.Link,
)

class GetRealSpanInfoLinkTypeUseCase(
    private val getProjectFileUseCase: GetProjectFileUseCase,
) : UseCase<GetRealSpanInfoLinkTypeInput, SpanInfo.Link.LinkType> {
    override suspend fun invoke(input: GetRealSpanInfoLinkTypeInput): SpanInfo.Link.LinkType {
        when (val linkType = input.span.linkType) {
            SpanInfo.Link.LinkType.NOTE, SpanInfo.Link.LinkType.FILE -> {
                val projectFile = getProjectFileUseCase(
                    GetProjectFileInput(
                        project = input.project,
                        relativePath = RelativePath(input.span.payload),
                    ),
                )
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
