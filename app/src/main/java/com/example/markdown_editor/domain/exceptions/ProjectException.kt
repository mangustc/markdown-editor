package com.example.markdown_editor.domain.exceptions

sealed class ProjectException(message: String, cause: Throwable? = null) : Exception(message, cause)

class FileNotFoundException(val path: String, cause: Throwable? = null) :
    ProjectException("File not found at: $path", cause)

class FileNotReadableException(val path: String, cause: Throwable? = null) :
    ProjectException("Failed to read file: $path", cause)

class FileNotWritableException(val path: String, cause: Throwable? = null) :
    ProjectException("Failed to write file: $path", cause)

class ProjectAccessException(val path: String, cause: Throwable? = null) :
    ProjectException("Cannot access project directory: $path", cause)