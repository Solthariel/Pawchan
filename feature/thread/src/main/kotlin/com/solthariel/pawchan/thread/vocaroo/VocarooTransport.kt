package com.solthariel.pawchan.thread.vocaroo

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Одна попытка скачать запись: [attempt] начинается с 1 и нужен транспорту для выбора пути и логов. */
data class VocarooRequest(
    val id: String,
    val url: String,
    val attempt: Int,
)

/**
 * Сеть для записей Vocaroo. Приложение даёт свой транспорт (DoH, Cronet, журнал соединений);
 * тред знает только этот интерфейс и не зависит от сетевого слоя.
 */
fun interface VocarooTransport {
    /** Качает запись в [target] целиком; при ошибке бросает [IOException]. */
    suspend fun download(
        request: VocarooRequest,
        target: File,
        onProgress: (Float?) -> Unit,
    )
}

/** Обычный OkHttp-транспорт; [client] уже настроен приложением (DNS, таймауты, журнал). */
class OkHttpVocarooTransport(
    private val client: OkHttpClient,
) : VocarooTransport {
    override suspend fun download(
        request: VocarooRequest,
        target: File,
        onProgress: (Float?) -> Unit,
    ) {
        val call =
            client.newCall(
                Request
                    .Builder()
                    .url(request.url)
                    .header("User-Agent", USER_AGENT)
                    .tag(VocarooRequest::class.java, request)
                    .build(),
            )
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) = continuation.resumeWithException(e)

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        try {
                            response.use { store(it, target, onProgress) }
                            continuation.resume(Unit)
                        } catch (failure: IOException) {
                            continuation.resumeWithException(failure)
                        }
                    }
                },
            )
        }
    }

    private fun store(
        response: Response,
        target: File,
        onProgress: (Float?) -> Unit,
    ) {
        if (!response.isSuccessful) throw VocarooHttpException(response.code)
        val body = response.body
        body.byteStream().use { copyVocarooBody(it, body.contentLength(), target, onProgress) }
    }
}

/** Сервер ответил не 2xx: повторять имеет смысл только при 5xx и 429. */
class VocarooHttpException(
    val code: Int,
) : IOException("HTTP $code")

/** Копирует тело ответа в [target]; пустой ответ — ошибка, чтобы в кэш не попал битый файл. */
fun copyVocarooBody(
    input: InputStream,
    total: Long,
    target: File,
    onProgress: (Float?) -> Unit,
) {
    var copied = 0L
    target.outputStream().use { output ->
        val buffer = ByteArray(BUFFER_BYTES)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            onProgress(if (total > 0L) copied.toFloat() / total else null)
        }
    }
    if (copied == 0L) throw IOException("Empty body")
    if (total > 0L && copied < total) throw IOException("Truncated body: $copied of $total")
}

internal const val USER_AGENT = "Mozilla/5.0 (Android) Pawchan"
private const val BUFFER_BYTES = 16 * 1024
