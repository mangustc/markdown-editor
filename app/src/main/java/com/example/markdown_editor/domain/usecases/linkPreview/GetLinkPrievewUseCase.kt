package com.example.markdown_editor.domain.usecases.linkPreview

import com.example.markdown_editor.data.linkPreview.LinkPreviewRepository
import com.example.markdown_editor.domain.models.LinkPreview
import com.example.markdown_editor.domain.usecases.UseCase

data class GetLinkPreviewInput(
    val url: String,
)

class GetLinkPreviewUseCase(
    private val linkPreviewRepository: LinkPreviewRepository,
) : UseCase<GetLinkPreviewInput, LinkPreview?> {
    override suspend fun invoke(input: GetLinkPreviewInput): LinkPreview? {
        return linkPreviewRepository.getLinkPreview(input.url)
    }
}
