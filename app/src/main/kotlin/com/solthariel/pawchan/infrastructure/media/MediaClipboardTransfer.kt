package com.solthariel.pawchan.infrastructure.media

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.coroutineContext

internal suspend fun writeClipboardImage(
    client: OkHttpClient,
    request: Request,
    target: File,
) = withContext(Dispatchers.IO) {
    coroutineScope {
        val call = client.newCall(request)
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
                if (!response.isSuccessful) throw IOException("Clipboard image HTTP ${response.code}")
                val body = response.body
                if (body.contentLength() > MAX_CLIPBOARD_IMAGE_BYTES) throw IOException("Clipboard image exceeds limit")
                val directory = checkNotNull(target.parentFile)
                directory.mkdirs()
                val staging = File.createTempFile("clipboard-", ".part", directory)
                try {
                    body.byteStream().use { input ->
                        staging.outputStream().use { output -> copyClipboardBytes(input, output) }
                    }
                    coroutineContext.ensureActive()
                    Files.move(
                        staging.toPath(),
                        target.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                } finally {
                    staging.delete()
                }
            }
        } finally {
            cancellation.cancel()
        }
    }
}

internal suspend fun copyClipboardBytes(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    limit: Long = MAX_CLIPBOARD_IMAGE_BYTES,
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        coroutineContext.ensureActive()
        val size = input.read(buffer)
        if (size < 0) return
        total += size
        if (total > limit) throw IOException("Clipboard image exceeds limit")
        output.write(buffer, 0, size)
    }
}

private const val MAX_CLIPBOARD_IMAGE_BYTES = 50L * 1024L * 1024L
