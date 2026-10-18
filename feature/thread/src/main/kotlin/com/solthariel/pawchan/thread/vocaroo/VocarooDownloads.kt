package com.solthariel.pawchan.thread.vocaroo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException

/**
 * Запись Vocaroo качается своим транспортом, а не встроенным в MediaPlayer: у того таймаут 8 с на
 * каждый адрес и перебор адресов по очереди. Пока на кнопке крутится загрузка, неудачные попытки
 * тихо повторяются (пауза 1–2 с) в пределах [VocarooRetryPolicy.budgetMillis]; файл кладётся в кэш
 * и играется локально.
 */
internal class VocarooDownloads(
    private val transport: VocarooTransport,
    private val directory: File,
    private val policy: VocarooRetryPolicy = VocarooRetryPolicy(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = { Log.i(TAG, it) },
) {
    fun cached(id: String): File? = file(id).takeIf { it.isFile && it.length() > 0L }

    /** [onProgress] — доля скачанного или null, если размер неизвестен; вызывается на сетевом потоке. */
    suspend fun fetch(
        id: String,
        onProgress: (Float?) -> Unit,
    ): File? = withContext(Dispatchers.IO) { retrying(id, onProgress) }

    private suspend fun retrying(
        id: String,
        onProgress: (Float?) -> Unit,
    ): File? {
        val started = nowMillis()
        var attempt = 1
        var failure = tryOnce(VocarooRequest(id, vocarooMp3Url(id), attempt), policy.attemptMillis(0L), onProgress)
        while (failure != null && policy.shouldRetry(failure, nowMillis() - started + policy.pauseMillis(attempt))) {
            val pause = policy.pauseMillis(attempt)
            log("record=$id attempt=$attempt failed after ${nowMillis() - started}ms, retry in ${pause}ms")
            onProgress(null)
            delay(pause)
            attempt++
            val request = VocarooRequest(id, vocarooMp3Url(id), attempt)
            failure = tryOnce(request, policy.attemptMillis(nowMillis() - started), onProgress)
        }
        if (failure == null) {
            log("record=$id ready after $attempt attempts, ${nowMillis() - started}ms")
            return file(id)
        }
        log("record=$id gave up after $attempt attempts, ${nowMillis() - started}ms: ${failure.describe()}")
        return null
    }

    /** Возвращает ошибку попытки или null, если файл сохранён в кэш. */
    private suspend fun tryOnce(
        request: VocarooRequest,
        timeoutMillis: Long,
        onProgress: (Float?) -> Unit,
    ): Throwable? {
        directory.mkdirs()
        val partial = File(directory, "${request.id}.part")
        return try {
            withTimeout(timeoutMillis) { transport.download(request, partial, onProgress) }
            val stored = partial.renameTo(file(request.id))
            if (stored) trim() else partial.delete()
            if (stored) null else IOException("Cache rename failed")
        } catch (failure: IOException) {
            partial.delete()
            failure
        } catch (timeout: TimeoutCancellationException) {
            partial.delete()
            timeout
        }
    }

    /** Кэш небольшой: хранятся только последние записи. */
    private fun trim() {
        val files = directory.listFiles { file -> file.name.endsWith(MP3_SUFFIX) }.orEmpty()
        files
            .sortedByDescending(File::lastModified)
            .drop(CACHED_RECORDS)
            .forEach(File::delete)
    }

    private fun file(id: String) = File(directory, id + MP3_SUFFIX)
}

/**
 * Повторы, пока пользователь видит загрузку: общий бюджет ~40 с, паузы 1 с после первой попытки
 * и 2 с дальше. Ответы 4xx (кроме 429) не повторяются — запись удалена или адрес неверный.
 */
internal data class VocarooRetryPolicy(
    val budgetMillis: Long = BUDGET_MILLIS,
    val maxAttemptMillis: Long = ATTEMPT_MILLIS,
    val minAttemptMillis: Long = MIN_ATTEMPT_MILLIS,
) {
    fun pauseMillis(attempt: Int): Long = if (attempt <= 1) FIRST_PAUSE_MILLIS else NEXT_PAUSE_MILLIS

    fun attemptMillis(elapsedMillis: Long): Long {
        val remaining = budgetMillis - elapsedMillis
        return remaining.coerceIn(minAttemptMillis, maxAttemptMillis)
    }

    fun shouldRetry(
        failure: Throwable,
        elapsedWithPauseMillis: Long,
    ): Boolean {
        val code = (failure as? VocarooHttpException)?.code
        val permanent = code != null && code in CLIENT_ERRORS && code != TOO_MANY_REQUESTS
        return !permanent && elapsedWithPauseMillis + minAttemptMillis <= budgetMillis
    }
}

private fun Throwable.describe(): String = "${javaClass.simpleName}: $message"

internal const val TAG = "Vocaroo"
private const val MP3_SUFFIX = ".mp3"
private const val CACHED_RECORDS = 30
private const val BUDGET_MILLIS = 40_000L
private const val ATTEMPT_MILLIS = 20_000L
private const val MIN_ATTEMPT_MILLIS = 3_000L
private const val FIRST_PAUSE_MILLIS = 1_000L
private const val NEXT_PAUSE_MILLIS = 2_000L
private const val TOO_MANY_REQUESTS = 429
private const val FIRST_CLIENT_ERROR = 400
private const val LAST_CLIENT_ERROR = 499
private val CLIENT_ERRORS = FIRST_CLIENT_ERROR..LAST_CLIENT_ERROR
