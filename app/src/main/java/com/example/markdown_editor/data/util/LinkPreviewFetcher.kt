package com.example.markdown_editor.data.util

import com.example.markdown_editor.data.model.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object LinkPreviewFetcher {

    private val URL_REGEX = Regex("""https?://[^\s<>"'\)]+""")

    fun extractFirstUrl(text: String): String? = URL_REGEX.find(text)?.value

    fun extractAllUrls(text: String): List<String> =
        URL_REGEX.findAll(text).map { it.value }.distinct().toList()

    suspend fun fetch(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (compatible; LinkPreviewBot/1.0)"
            )
            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            connection.disconnect()

            fun og(property: String): String? {
                // handles both attribute orderings
                val a = Regex(
                    """<meta[^>]+property=["']og:$property["'][^>]+content=["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                ).find(html)?.groupValues?.get(1)
                if (a != null) return a
                return Regex(
                    """<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:$property["']""",
                    RegexOption.IGNORE_CASE
                ).find(html)?.groupValues?.get(1)
            }

            // Fallback: <title> tag if og:title absent
            val title = og("title") ?: Regex(
                """<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE
            ).find(html)?.groupValues?.get(1)?.trim()

            LinkPreview(
                url = url,
                title = title,
                description = og("description"),
                imageUrl = og("image"),
            )
        } catch (e: Exception) {
            null
        }
    }
}