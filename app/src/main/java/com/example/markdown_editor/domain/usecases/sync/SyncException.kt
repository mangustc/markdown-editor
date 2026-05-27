package com.example.markdown_editor.domain.usecases.sync

sealed class SyncException(val userMessage: String) : Exception(userMessage)

class SyncAuthException : SyncException("Authentication failed. Please log in again.")
class SyncNetworkException : SyncException("Network error. Check internet connection.")
class SyncServerException : SyncException("Sync server unavailable. Try again later.")
class SyncLocalIoException : SyncException("Local file error. Check storage space and permissions.")
class SyncStateException : SyncException("Sync data corrupted. Please reset sync.")
class SyncQuotaException : SyncException("Cloud storage full. Free up space.")
