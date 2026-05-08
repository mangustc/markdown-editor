package com.example.markdown_editor.domain.messenger

object LinkPreviewFetcher {
    private val URL_REGEX = Regex("""https?://[^\s<>"'\)]+""")

    fun extractAllUrls(text: String): List<String> =
        URL_REGEX.findAll(text).map { it.value }.distinct().toList()
}