package com.example.markdown_editor.data.linkPreview

import com.example.markdown_editor.data.database.LinkPreviewDao
import com.example.markdown_editor.data.database.LinkPreviewEntity
import com.example.markdown_editor.domain.models.LinkPreview
import com.example.markdown_editor.domain.repositories.LinkPreviewRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidLinkPreviewRepository(
    private val linkPreviewDao: LinkPreviewDao,
    private val client: HttpClient,
) : LinkPreviewRepository {

    override suspend fun getLinkPreview(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        val linkPreviewCached = linkPreviewDao.getByUrl(url)

        if (linkPreviewCached == null) {
            val html = client.get(url) {
                header("User-Agent", "Mozilla/5.0 (compatible; LinkPreviewBot/1.0)")
                timeout {
                    requestTimeoutMillis = 6_000
                    connectTimeoutMillis = 6_000
                }
            }.bodyAsText()

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
        } else {
            if (linkPreviewCached.title == null && linkPreviewCached.description == null && linkPreviewCached.imageUrl == null)
                null
            else
                LinkPreview(
                    url = linkPreviewCached.url,
                    title = linkPreviewCached.title,
                    description = linkPreviewCached.description,
                    imageUrl = linkPreviewCached.imageUrl,
                )
        }
    }
}