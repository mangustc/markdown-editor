package com.example.markdown_editor.data.sync

import com.example.markdown_editor.domain.exceptions.SyncAuthException
import com.example.markdown_editor.domain.exceptions.SyncNetworkException
import com.example.markdown_editor.domain.exceptions.SyncQuotaException
import com.example.markdown_editor.domain.exceptions.SyncServerException
import com.example.markdown_editor.domain.models.RelativePath
import com.example.markdown_editor.domain.repositories.SyncRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class YandexSyncRepository(
    private val oauthToken: String,
    private val client: HttpClient,
) : SyncRepository {

    override val name: String = "Yandex Disk"

    private val json = Json { ignoreUnknownKeys = true }
    private val baseApi = "https://cloud-api.yandex.net/v1/disk"
    private val appFolder = "app:"

    override suspend fun downloadFile(path: RelativePath): ByteArray? =
        withContext(Dispatchers.IO) {
            runNetwork {
                val encodedPath = encode("$appFolder/$path")
                val downloadUrl = getDownloadUrl(encodedPath) ?: return@runNetwork null

                val response = client.get(downloadUrl) {
                    header("Authorization", "OAuth $oauthToken")
                }

                if (response.status.value == 404) return@runNetwork null
                checkError(response)
                response.readRawBytes()
            }
        }

    override suspend fun uploadFile(path: RelativePath, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            try {
                tryUploadFile(path, bytes)
            } catch (_: Exception) {
                ensureDirectories(path)
                tryUploadFile(path, bytes)
            }
        }

    private suspend fun tryUploadFile(path: RelativePath, bytes: ByteArray) = runNetwork {
        val encodedPath = encode("$appFolder/$path")
        val uploadUrl = getUploadUrl(encodedPath)

        val response = client.put(uploadUrl) {
            contentType(ContentType.Application.OctetStream)
            setBody(bytes)
        }

        if (response.status.value != 201 && response.status.value != 202) {
            checkError(response)
            throw SyncServerException()
        }
    }

    override suspend fun deleteFile(path: RelativePath) = withContext(Dispatchers.IO) {
        runNetwork {
            val encodedPath = encode("$appFolder/$path")
            val response = client.delete("$baseApi/resources?path=$encodedPath&permanently=true") {
                header("Authorization", "OAuth $oauthToken")
            }

            if (!response.status.isSuccess() && response.status.value != 404) {
                checkError(response)
            }
        }
    }

    private suspend fun getDownloadUrl(encodedPath: String): String? {
        val response = client.get("$baseApi/resources/download?path=$encodedPath") {
            header("Authorization", "OAuth $oauthToken")
        }

        if (response.status.value == 404) return null
        checkError(response)
        val body = response.bodyAsText()
        return json.decodeFromString<YaDiskHref>(body).href
    }

    private suspend fun getUploadUrl(encodedPath: String): String {
        val response = client.get("$baseApi/resources/upload?path=$encodedPath&overwrite=true") {
            header("Authorization", "OAuth $oauthToken")
        }

        checkError(response)
        val body = response.bodyAsText()
        return json.decodeFromString<YaDiskHref>(body).href
    }

    private suspend fun ensureDirectories(relativePath: RelativePath) {
        val parts = relativePath.dirRelativePath.splitParts()
        var current = appFolder
        parts.forEach { segment ->
            current += "/$segment"
            val encoded = encode(current)

            val response = client.put("$baseApi/resources?path=$encoded") {
                header("Authorization", "OAuth $oauthToken")
                setBody(EmptyContent)
            }

            if (response.status.value !in listOf(201, 409)) {
                checkError(response)
                throw SyncServerException()
            }
        }
    }

    private fun encode(s: String) = s.encodeURLQueryComponent()

    private suspend inline fun <T> runNetwork(crossinline block: suspend () -> T): T {
        return try {
            block()
        } catch (_: RuntimeException) {
            throw SyncNetworkException()
        }
    }

    private fun checkError(response: HttpResponse) {
        if (response.status.isSuccess()) return
        when (response.status.value) {
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