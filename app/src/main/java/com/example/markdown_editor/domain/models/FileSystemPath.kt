package com.example.markdown_editor.domain.models

data class FileSystemPath(val value: String) {
    override fun toString(): String {
        return value
    }
}