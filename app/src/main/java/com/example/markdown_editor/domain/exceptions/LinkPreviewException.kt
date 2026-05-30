package com.example.markdown_editor.domain.exceptions

sealed class LinkPreviewException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class LinkFetchException(val path: String, cause: Throwable? = null) :
    LinkPreviewException("Failed to fetch link information: $path", cause)
