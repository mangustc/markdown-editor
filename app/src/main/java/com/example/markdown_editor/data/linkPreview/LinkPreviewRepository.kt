package com.example.markdown_editor.data.linkPreview

import com.example.markdown_editor.domain.models.LinkPreview

interface LinkPreviewRepository {
    suspend fun getLinkPreview(url: String): LinkPreview?
}