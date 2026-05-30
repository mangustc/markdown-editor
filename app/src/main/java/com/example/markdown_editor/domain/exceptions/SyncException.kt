package com.example.markdown_editor.domain.exceptions

sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

class SyncAuthException : SyncException("Authentication failed. Please log in again.")
class SyncNetworkException : SyncException("Network error. Check internet connection.")
class SyncServerException : SyncException("Sync server unavailable. Try again later.")
class SyncStateException : SyncException("Sync data corrupted. Please reset sync.")
class SyncQuotaException : SyncException("Cloud storage full. Free up space.")
