package com.example.markdown_editor.data.sync

import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.usecases.sync.SyncAuthException
import com.example.markdown_editor.domain.usecases.sync.SyncNetworkException
import com.example.markdown_editor.domain.usecases.sync.SyncQuotaException
import com.example.markdown_editor.domain.usecases.sync.SyncServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder

class YandexSyncRepository(
    private val oauthToken: String,
) : SyncRepository {

    override val name: String = "Yandex Disk"

    private val json = Json { ignoreUnknownKeys = true }
    private val baseApi = "https://cloud-api.yandex.net/v1/disk"
    private val appFolder = "app:"

    private val client = OkHttpClient()

    override suspend fun downloadFile(path: RelativePath): ByteArray? =
        withContext(Dispatchers.IO) {
            runNetwork {
                val encodedPath = encode("$appFolder/$path")
                val downloadUrl = getDownloadUrl(encodedPath) ?: return@runNetwork null

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "OAuth $oauthToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 404) return@runNetwork null
                    checkError(response)
                    response.body.bytes()
                }
            }
        }

    override suspend fun uploadFile(path: RelativePath, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            runNetwork {
                val encodedPath = encode("$appFolder/$path")
                ensureDirectories(path)
                val uploadUrl = getUploadUrl(encodedPath)

                val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code != 201 && response.code != 202) {
                        checkError(response)
                        throw SyncServerException()
                    }
                }
            }
        }

    override suspend fun deleteFile(path: RelativePath) = withContext(Dispatchers.IO) {
        runNetwork {
            val encodedPath = encode("$appFolder/$path")
            val request = Request.Builder()
                .url("$baseApi/resources?path=$encodedPath&permanently=true")
                .header("Authorization", "OAuth $oauthToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    checkError(response)
                }
            }
        }
    }

    private fun getDownloadUrl(encodedPath: String): String? {
        val request = Request.Builder()
            .url("$baseApi/resources/download?path=$encodedPath")
            .header("Authorization", "OAuth $oauthToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            checkError(response)
            val body = response.body.string()
            return json.decodeFromString<YaDiskHref>(body).href
        }
    }

    private fun getUploadUrl(encodedPath: String): String {
        val request = Request.Builder()
            .url("$baseApi/resources/upload?path=$encodedPath&overwrite=true")
            .header("Authorization", "OAuth $oauthToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkError(response)
            val body = response.body.string()
            return json.decodeFromString<YaDiskHref>(body).href
        }
    }

    private fun ensureDirectories(relativePath: RelativePath) {
        val parts = relativePath.dirRelativePath.splitParts()
        var current = appFolder
        parts.forEach { segment ->
            current += "/$segment"
            val encoded = encode(current)
            val request = Request.Builder()
                .url("$baseApi/resources?path=$encoded")
                .header("Authorization", "OAuth $oauthToken")
                .put(RequestBody.EMPTY)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code !in listOf(201, 409)) {
                    checkError(response)
                    throw SyncServerException()
                }
            }
        }
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")

    private inline fun <T> runNetwork(block: () -> T): T {
        return try {
            block()
        } catch (_: IOException) {
            throw SyncNetworkException()
        }
    }

    private fun checkError(response: Response) {
        if (response.isSuccessful) return
        when (response.code) {
            401, 403 -> throw SyncAuthException()
            413, 507 -> throw SyncQuotaException()
            in 500..599 -> throw SyncServerException()
            else -> throw SyncNetworkException()
        }
    }

    @Serializable
    private data class YaDiskHref(val href: String)

    @Serializable
    private data class YaDiskResourceResponse(val embedded: YaDiskEmbedded? = null)

    @Serializable
    private data class YaDiskEmbedded(val items: List<YaDiskItem> = emptyList())

    @Serializable
    private data class YaDiskItem(
        val path: String,
        val type: String,
        val name: String,
        val md5: String? = null,
    )
}