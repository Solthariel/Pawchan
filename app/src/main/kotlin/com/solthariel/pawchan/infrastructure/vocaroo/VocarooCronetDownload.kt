package com.solthariel.pawchan.infrastructure.vocaroo

import com.solthariel.pawchan.thread.vocaroo.VocarooHttpException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Вызовы Cronet и его callbacks идут последовательно в одном потоке, включая отмену. */
internal suspend fun downloadWithCronet(
    target: File,
    onProgress: (Float?) -> Unit,
    createRequest: (UrlRequest.Callback, Executor) -> UrlRequest,
) = suspendCancellableCoroutine { continuation ->
    val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "vocaroo-cronet-download") }
    val callback = CronetDownloadCallback(target, onProgress, continuation, executor)
    var request: UrlRequest? = null // Доступ только из executor.
    executor.execute {
        if (!continuation.isActive) {
            executor.shutdown()
        } else {
            try {
                request = createRequest(callback, executor)
                requireNotNull(request).start()
            } catch (failure: CancellationException) {
                continuation.cancel(failure)
                executor.shutdown()
            } catch (failure: IllegalArgumentException) {
                callback.startFailed(failure)
            } catch (failure: IllegalStateException) {
                callback.startFailed(failure)
            } catch (failure: SecurityException) {
                callback.startFailed(failure)
            } catch (failure: RejectedExecutionException) {
                callback.startFailed(failure)
            }
        }
    }
    continuation.invokeOnCancellation {
        try {
            executor.execute {
                callback.closeQuietly()
                val activeRequest = request
                if (activeRequest != null) activeRequest.cancel() else executor.shutdown()
            }
        } catch (_: RejectedExecutionException) {
            // Запрос уже завершился или был отменён до запуска.
        }
    }
}

private class CronetDownloadCallback(
    private val target: File,
    private val onProgress: (Float?) -> Unit,
    private val continuation: CancellableContinuation<Unit>,
    private val executor: ExecutorService,
) : UrlRequest.Callback() {
    private var output: OutputStream? = null
    private var copied = 0L
    private var total = -1L
    private val bytes = ByteArray(BUFFER_BYTES)

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String,
    ) {
        if (continuation.isActive) request.followRedirect()
    }

    override fun onResponseStarted(
        request: UrlRequest,
        info: UrlResponseInfo,
    ) {
        if (!continuation.isActive) return
        if (info.httpStatusCode !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
            request.cancel()
            continuation.resumeWithException(VocarooHttpException(info.httpStatusCode))
            return
        }
        total = info.allHeaders.entries
            .firstOrNull { it.key.equals("Content-Length", ignoreCase = true) }
            ?.value
            ?.firstOrNull()
            ?.toLongOrNull() ?: -1L
        output = target.outputStream()
        request.read(ByteBuffer.allocateDirect(BUFFER_BYTES))
    }

    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer,
    ) {
        if (!continuation.isActive) return
        byteBuffer.flip()
        val count = byteBuffer.remaining()
        byteBuffer.get(bytes, 0, count)
        requireNotNull(output).write(bytes, 0, count)
        copied += count
        onProgress(if (total > 0L) copied.toFloat() / total else null)
        byteBuffer.clear()
        request.read(byteBuffer)
    }

    override fun onSucceeded(
        request: UrlRequest,
        info: UrlResponseInfo,
    ) {
        try {
            output?.close()
            output = null
            if (copied == 0L) throw IOException("Empty body")
            if (total > 0L && copied < total) throw IOException("Truncated body: $copied of $total")
            continuation.resume(Unit)
        } catch (failure: IOException) {
            continuation.resumeWithException(failure)
        } finally {
            closeQuietly()
            executor.shutdown()
        }
    }

    override fun onFailed(
        request: UrlRequest,
        info: UrlResponseInfo?,
        error: CronetException,
    ) {
        closeQuietly()
        continuation.resumeWithException(error)
        executor.shutdown()
    }

    override fun onCanceled(
        request: UrlRequest,
        info: UrlResponseInfo?,
    ) {
        closeQuietly()
        executor.shutdown()
    }

    fun startFailed(failure: RuntimeException) {
        closeQuietly()
        continuation.resumeWithException(IOException("Cannot start Cronet request", failure))
        executor.shutdown()
    }

    fun closeQuietly() {
        try {
            output?.close()
        } catch (_: IOException) {
            // Основную причину ошибки или отмены не подменяем ошибкой закрытия.
        }
        output = null
        if (continuation.isCancelled) target.delete()
    }
}

private const val BUFFER_BYTES = 16 * 1024
