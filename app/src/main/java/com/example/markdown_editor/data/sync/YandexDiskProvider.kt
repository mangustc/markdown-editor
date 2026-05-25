package com.example.markdown_editor.data.sync

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

class YandexDiskProvider(
    private val oauthToken: String,
) : SyncProvider {

    override val name: String = "Yandex Disk"

    private val json = Json { ignoreUnknownKeys = true }
    private val baseApi = "https://cloud-api.yandex.net/v1/disk"
    private val appFolder = "app:"

    private val client = OkHttpClient()

    override suspend fun listRemoteFiles(remoteRoot: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            runNetwork {
                val rootPath = "$appFolder/$remoteRoot"
                val result = mutableMapOf<String, String>()
                listRecursive(rootPath, remoteRoot, result)
                result
            }
        }

    override suspend fun downloadFile(remotePath: String): ByteArray? =
        withContext(Dispatchers.IO) {
            runNetwork {
                val encodedPath = encode("$appFolder/$remotePath")
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

    override suspend fun uploadFile(remotePath: String, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            runNetwork {
                val encodedPath = encode("$appFolder/$remotePath")
                ensureDirectories(remotePath)
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

    override suspend fun deleteFile(remotePath: String) = withContext(Dispatchers.IO) {
        runNetwork {
            val encodedPath = encode("$appFolder/$remotePath")
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

    override suspend fun testConnection() = withContext(Dispatchers.IO) {
        runNetwork {
            val request = Request.Builder()
                .url("$baseApi/")
                .header("Authorization", "OAuth $oauthToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                checkError(response)
            }
        }
    }

    private fun listRecursive(
        diskPath: String,
        remoteRoot: String,
        out: MutableMap<String, String>,
    ) {
        val encodedPath = encode(diskPath)
        val url =
            "$baseApi/resources?path=$encodedPath&limit=1000&fields=_embedded.items.path,_embedded.items.md5,_embedded.items.type,_embedded.items.name"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "OAuth $oauthToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            checkError(response)
            val body = response.body.string()
            val resourceResponse = json.decodeFromString<YaDiskResourceResponse>(body)

            resourceResponse.embedded?.items?.forEach { item ->
                when (item.type) {
                    "dir" -> listRecursive(item.path, remoteRoot, out)
                    "file" -> {
                        val prefix = "disk:$appFolder/$remoteRoot/"
                        val rel = item.path.removePrefix(prefix)
                        item.md5?.let { out[rel] = it }
                    }
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

    private fun ensureDirectories(relativePath: String) {
        val parts = relativePath.split("/").dropLast(1)
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

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

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

    // ── Serialization models ─────────────────────────────────────────────

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