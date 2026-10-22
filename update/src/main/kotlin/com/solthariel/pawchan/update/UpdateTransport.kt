package com.solthariel.pawchan.update

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Reads bounded responses on IO; a structured child cancels the call until body close. */
internal class UpdateTransport(
    private val client: OkHttpClient,
) : AutoCloseable {
    private val closed = AtomicBoolean()

    suspend fun readText(url: HttpUrl): String =
        execute(url) { response, checkpoint ->
            val body = checkNotNull(response.body)
            check(body.contentLength() <= MAX_UPDATE_TEXT_BYTES) { "Update text exceeds size limit" }
            val source = body.source()
            source.request(MAX_UPDATE_TEXT_BYTES + 1)
            checkpoint()
            check(source.buffer.size <= MAX_UPDATE_TEXT_BYTES) { "Update text exceeds size limit" }
            body.string()
        }

    suspend fun downloadTo(
        url: HttpUrl,
        target: File,
        expectedLength: Long,
        onProgress: (Int) -> Unit,
    ) {
        execute(url) { response, checkpoint ->
            val body = checkNotNull(response.body)
            val length = body.contentLength()
            check(length < 0 || length == expectedLength) { "Update Content-Length mismatch" }
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    copyUpdateStream(input, output, expectedLength, onProgress, checkpoint)
                }
            }
        }
    }

    private suspend fun <T> execute(
        url: HttpUrl,
        read: (Response, () -> Unit) -> T,
    ): T =
        withContext(Dispatchers.IO) {
            coroutineScope {
                check(!closed.get()) { "Update transport is closed" }
                val call = client.newCall(Request.Builder().url(url).build())
                val cancellation =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            awaitCancellation()
                        } finally {
                            call.cancel()
                        }
                    }
                try {
                    val context = currentCoroutineContext()
                    call.executeAsync().use { response ->
                        context.ensureActive()
                        check(response.isSuccessful) { "Update request failed: ${response.code}" }
                        read(response) { context.ensureActive() }
                    }
                } catch (error: IOException) {
                    currentCoroutineContext().ensureActive()
                    throw error
                } finally {
                    cancellation.cancel()
                }
            }
        }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        client.dispatcher.cancelAll()
        // This transport owns the executor. Cleanup must not close TLS sockets on Main.
        client.dispatcher.executorService.execute {
            client.connectionPool.evictAll()
            client.cache?.close()
        }
        client.dispatcher.executorService.shutdown()
    }
}

internal const val MAX_UPDATE_TEXT_BYTES = 1024L * 1024L
