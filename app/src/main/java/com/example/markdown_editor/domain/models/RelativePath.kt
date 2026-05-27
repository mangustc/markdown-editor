package com.example.markdown_editor.domain.models

/**
 * Relative parts delimiters should always be "/"
 *
 * Examples:
 *
 * - Files: "hello/world.md", ".sync_manifest.json"
 * - Directories: "hello", "", "assets/dir"
 */
data class RelativePath(
    val value: String,
) {
    override fun toString(): String {
        return value
    }

    fun splitParts(): List<String> {
        return value.split("/")
    }

    fun appendRelativePath(relativePath: RelativePath): RelativePath {
        if (value.isEmpty()) return relativePath
        return RelativePath("$value/${relativePath.value}")
    }

    val basename: String get() = splitParts().last()

    val dirRelativePath: RelativePath
        get() = RelativePath(
            value = splitParts().dropLast(1).joinToString("/"),
        )
}
