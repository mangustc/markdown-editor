package com.example.markdown_editor.domain.repositories

import com.example.markdown_editor.domain.models.LinkPreview

interface LinkPreviewRepository {
    suspend fun getLinkPreview(url: String): LinkPreview?
}