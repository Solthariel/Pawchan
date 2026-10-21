package com.solthariel.pawchan.settings

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class ChangelogDownload(
    val body: String?,
    val etag: String?,
)

internal fun historyUrl(manifest: String): String? =
    manifest
        .toHttpUrlOrNull()
        ?.takeIf { it.isHttps }
        ?.resolve("history-v1.json")
        ?.toString()

/** Only text from the same HTTPS update host; no release assets or extra authentication. */
internal class ChangelogHttp(
    private val client: OkHttpClient = historyHttpClient,
) {
    suspend fun download(
        url: String,
        etag: String?,
        minimumCode: Long? = null,
    ): ChangelogDownload {
        val request =
            Request
                .Builder()
                .url(historyRequestUrl(url, minimumCode))
                .apply {
                    etag?.let { header("If-None-Match", it) }
                    if (minimumCode != null) header("Cache-Control", "no-cache")
                }.build()
        return client.newCall(request).executeAsync().use { response ->
            if (response.code == HTTP_NOT_MODIFIED) return@use ChangelogDownload(null, etag)
            if (!response.isSuccessful) throw IOException("History request failed: ${response.code}")
            val body = response.body ?: throw IOException("Empty history response")
            require(body.contentLength() <= MAX_HISTORY_BYTES) { "History exceeds limit" }
            val source = body.source()
            source.request(MAX_HISTORY_BYTES.toLong() + 1)
            require(source.buffer.size <= MAX_HISTORY_BYTES) { "History exceeds limit" }
            val tag = response.header("ETag")?.takeIf { it.length <= MAX_ETAG_LENGTH }
            ChangelogDownload(source.readUtf8(), tag)
        }
    }
}

private const val HISTORY_TIMEOUT_SECONDS = 15L
private const val HTTP_NOT_MODIFIED = 304
private const val MAX_ETAG_LENGTH = 512

private val historyHttpClient =
    OkHttpClient
        .Builder()
        .callTimeout(HISTORY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followSslRedirects(false)
        .build()

private fun historyRequestUrl(
    url: String,
    minimumCode: Long?,
): String =
    if (minimumCode == null) {
        url
    } else {
        requireNotNull(url.toHttpUrlOrNull())
            .newBuilder()
            .setQueryParameter("release_code", minimumCode.toString())
            .build()
            .toString()
    }
