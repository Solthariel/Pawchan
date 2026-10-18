package com.solthariel.pawchan.infrastructure.vocaroo

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import com.solthariel.pawchan.thread.vocaroo.VocarooHttpException
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import com.solthariel.pawchan.thread.vocaroo.VocarooTransport
import com.solthariel.pawchan.thread.vocaroo.copyVocarooBody
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cronet из Google Play Services — только для Vocaroo: свой сетевой стек Chromium (HTTP/2, QUIC)
 * иногда проходит там, где OkHttp зависает на TLS. Без Play Services движка нет, работает OkHttp.
 */
internal class VocarooCronet(
    context: Context,
) : VocarooTransport {
    private val context = context.applicationContext
    private val engine = CompletableDeferred<CronetEngine?>()

    @Volatile
    private var current: CronetEngine? = null
    private val started = AtomicBoolean(false)

    /** Ждёт установки провайдера не дольше [waitMillis]; null — Cronet недоступен. */
    suspend fun ready(waitMillis: Long): Boolean {
        start()
        return withTimeoutOrNull(waitMillis) { engine.await() } != null
    }

    override suspend fun download(
        request: VocarooRequest,
        target: File,
        onProgress: (Float?) -> Unit,
    ) {
        val cronet = current ?: throw java.io.IOException("Cronet unavailable")
        val label = "record=${request.id} attempt=${request.attempt} cronet"
        val started = System.nanoTime()
        withContext(Dispatchers.IO) {
            val connection = cronet.openConnection(URL(request.url)) as HttpURLConnection
            val cancel = currentCoroutineContext().job.invokeOnCompletion { connection.disconnect() }
            try {
                Log.i(TAG, "$label start ${request.url}")
                connection.setRequestProperty("User-Agent", CRONET_USER_AGENT)
                val code = connection.responseCode
                Log.i(TAG, "$label http $code after ${elapsedMillis(started)}ms")
                if (code !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
                    throw VocarooHttpException(code)
                }
                val total = connection.contentLengthLong
                connection.inputStream.use { copyVocarooBody(it, total, target, onProgress) }
                Log.i(TAG, "$label body ${target.length()}B, total ${elapsedMillis(started)}ms")
            } catch (failure: java.io.IOException) {
                val reason = "${failure.javaClass.simpleName}: ${failure.message}"
                Log.w(TAG, "$label failed after ${elapsedMillis(started)}ms: $reason")
                throw failure
            } finally {
                cancel.dispose()
                connection.disconnect()
            }
        }
    }

    /** Закрытие движка не должно ронять приложение, если запрос ещё не завершился. */
    fun close() {
        val cronet = current ?: return
        try {
            cronet.shutdown()
        } catch (_: IllegalStateException) {
            Log.i(TAG, "cronet shutdown skipped: requests in flight")
        }
    }

    private fun start() {
        if (!started.compareAndSet(false, true)) return
        CronetProviderInstaller.installProvider(context).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.i(TAG, "cronet provider unavailable: ${task.exception?.javaClass?.simpleName}")
                engine.complete(null)
                return@addOnCompleteListener
            }
            Thread({
                current = build()
                engine.complete(current)
            }, "vocaroo-cronet").start()
        }
    }

    private fun build(): CronetEngine? {
        val provider =
            CronetProvider
                .getAllProviders(context)
                .firstOrNull { it.isEnabled && it.name != CronetProvider.PROVIDER_NAME_FALLBACK }
        val built =
            try {
                provider
                    ?.createBuilder()
                    ?.enableHttp2(true)
                    ?.enableQuic(true)
                    ?.build()
            } catch (failure: IllegalStateException) {
                Log.w(TAG, "cronet engine failed: ${failure.message}")
                null
            }
        val status = if (built != null) "ready: ${provider?.name} ${provider?.version}" else "unavailable"
        Log.i(TAG, "cronet engine $status")
        return built
    }
}

private const val CRONET_USER_AGENT = "Mozilla/5.0 (Android) Pawchan"
