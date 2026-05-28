package com.example.markdown_editor.domain.repositories

import com.example.markdown_editor.domain.models.RelativePath

interface SyncRepository {
    val name: String
    suspend fun downloadFile(path: RelativePath): ByteArray?
    suspend fun uploadFile(path: RelativePath, bytes: ByteArray)
    suspend fun deleteFile(path: RelativePath)
}