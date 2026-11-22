package com.solthariel.pawchan.media.video

import com.solthariel.pawchan.media.copyCancellableBytes
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.IOException

/** Full authenticated GET is a final fallback; no HTTP byte ranges are sent. */
internal suspend fun withLocalVideo(
    client: OkHttpClient,
    directory: File,
    url: String,
    onReady: (File) -> Unit,
) {
    withContext(Dispatchers.IO) {
        val file = File.createTempFile("neko-video-", ".media", directory)
        try {
            downloadLocalVideo(client, url, file)
            withContext(Dispatchers.Main.immediate) { onReady(file) }
            awaitCancellation()
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { file.delete() }
        }
    }
}

private suspend fun downloadLocalVideo(
    client: OkHttpClient,
    url: String,
    file: File,
) {
    withContext(Dispatchers.IO) {
        coroutineScope {
            val call =
                client.newCall(
                    Request
                        .Builder()
                        .url(url)
                        .get()
                        .build(),
                )
            val cancellation =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        call.cancel()
                    }
                }
            try {
                call.executeAsync().use { response ->
                    if (response.code != HTTP_OK) throw IOException("Video GET failed: HTTP ${response.code}")
                    val body = response.body
                    val expected = body.contentLength()
                    if (expected > VIDEO_LOCAL_MAX_BYTES) throw IOException("Video exceeds local fallback limit")
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            copyVideoBytes(input, output, expected)
                        }
                    }
                }
            } finally {
                cancellation.cancel()
            }
        }
    }
}

internal suspend fun copyVideoBytes(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    expected: Long,
) {
    val total = copyCancellableBytes(input, output, VIDEO_LOCAL_MAX_BYTES, "Video exceeds local fallback limit")
    if (total == 0L || (expected >= 0L && total != expected)) throw IOException("Incomplete video download")
}

private const val HTTP_OK = 200
private const val VIDEO_LOCAL_MAX_BYTES = 1024L * 1024L * 1024L
