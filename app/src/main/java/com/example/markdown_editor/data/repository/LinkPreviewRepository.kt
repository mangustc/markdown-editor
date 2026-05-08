package com.example.markdown_editor.data.repository

import com.example.markdown_editor.data.model.LinkPreview

interface LinkPreviewRepository {
    suspend fun getLinkPreview(url: String): LinkPreview?
}