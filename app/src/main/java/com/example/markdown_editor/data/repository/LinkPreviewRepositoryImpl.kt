package com.example.markdown_editor.data.repository

import android.util.Log
import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.LinkPreviewEntity
import com.example.markdown_editor.data.model.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LinkPreviewRepositoryImpl(
    private val linkPreviewDao: LinkPreviewDao,
) : LinkPreviewRepository {
    override suspend fun getLinkPreview(url: String): LinkPreview? =
        withContext(Dispatchers.IO) {
            linkPreviewDao.getByUrl(url)?.let { entity ->
                Log.d("debug", "null")
                if (entity.title == null && entity.description == null && entity.imageUrl == null)
                    null
                else
                    LinkPreview(
                        url = entity.url,
                        title = entity.title,
                        description = entity.description,
                        imageUrl = entity.imageUrl,
                    )
            } ?: try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 6_000
                connection.readTimeout = 6_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (compatible; LinkPreviewBot/1.0)",
                )
                val html = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                connection.disconnect()

                fun og(property: String): String? {
                    val a = Regex(
                        """<meta[^>]+property=["']og:$property["'][^>]+content=["']([^"']+)["']""",
                        RegexOption.IGNORE_CASE,
                    ).find(html)?.groupValues?.get(1)
                    if (a != null) return a
                    return Regex(
                        """<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:$property["']""",
                        RegexOption.IGNORE_CASE,
                    ).find(html)?.groupValues?.get(1)
                }

                val title = og("title") ?: Regex(
                    """<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE,
                ).find(html)?.groupValues?.get(1)?.trim()

                val preview = LinkPreview(
                    url = url,
                    title = title,
                    description = og("description"),
                    imageUrl = og("image"),
                )
                linkPreviewDao.insert(
                    LinkPreviewEntity(
                        url = preview.url,
                        title = preview.title,
                        description = preview.description,
                        imageUrl = preview.imageUrl,
                    ),
                )
                preview
            } catch (_: Exception) {
                null
            }
        }
}
