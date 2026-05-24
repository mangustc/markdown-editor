package com.example.markdown_editor.data.sync

interface SyncProvider {
    val name: String
    suspend fun listRemoteFiles(remoteRoot: String): Map<String, String>
    suspend fun downloadFile(remotePath: String): ByteArray?
    suspend fun uploadFile(remotePath: String, bytes: ByteArray)
    suspend fun deleteFile(remotePath: String)
    suspend fun testConnection()
}