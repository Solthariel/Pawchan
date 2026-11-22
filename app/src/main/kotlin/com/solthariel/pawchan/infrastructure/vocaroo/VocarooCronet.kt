package com.solthariel.pawchan.infrastructure.vocaroo

import android.content.Context
import android.util.Log
import com.google.android.gms.net.CronetProviderInstaller
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import com.solthariel.pawchan.thread.vocaroo.VocarooTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.chromium.net.CronetEngine
import org.chromium.net.CronetProvider
import java.io.File
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
    private val lifecycleLock = Any()
    private var closed = false

    /** Ждёт установки провайдера не дольше [waitMillis]; null — Cronet недоступен. */
    suspend fun ready(waitMillis: Long): Boolean {
        start()
        return withTimeoutOrNull(waitMillis) { engine.await() } != null && current != null
    }

    override suspend fun download(
        request: VocarooRequest,
        target: File,
        onProgress: (Float?) -> Unit,
    ) {
        val cronet = current ?: throw java.io.IOException("Cronet unavailable")
        val label = "record=${request.id} attempt=${request.attempt} cronet"
        val started = System.nanoTime()
        Log.i(TAG, "$label start ${request.url}")
        try {
            downloadWithCronet(
                target = target,
                onProgress = onProgress,
                createRequest = { callback, executor ->
                    cronet
                        .newUrlRequestBuilder(request.url, callback, executor)
                        .addHeader("User-Agent", CRONET_USER_AGENT)
                        .build()
                },
            )
            Log.i(TAG, "$label body ${target.length()}B, total ${elapsedMillis(started)}ms")
        } catch (failure: java.io.IOException) {
            val reason = "${failure.javaClass.simpleName}: ${failure.message}"
            Log.w(TAG, "$label failed after ${elapsedMillis(started)}ms: $reason")
            throw failure
        }
    }

    /** Закрытие движка не должно ронять приложение, если запрос ещё не завершился. */
    fun close() {
        val retired =
            synchronized(lifecycleLock) {
                closed = true
                engine.complete(null)
                current.also { current = null }
            }
        retired?.let(::shutDown)
    }

    private fun shutDown(cronet: CronetEngine) {
        try {
            cronet.shutdown()
        } catch (_: IllegalStateException) {
            Log.i(TAG, "cronet shutdown skipped: requests in flight")
        }
    }

    private fun start() {
        if (synchronized(lifecycleLock) { closed } || !started.compareAndSet(false, true)) return
        CronetProviderInstaller.installProvider(context).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.i(TAG, "cronet provider unavailable: ${task.exception?.javaClass?.simpleName}")
                engine.complete(null)
                return@addOnCompleteListener
            }
            Thread({
                val created = build()
                val retained =
                    synchronized(lifecycleLock) {
                        if (closed) {
                            false
                        } else {
                            current = created
                            engine.complete(created)
                            true
                        }
                    }
                if (!retained && created != null) shutDown(created)
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
