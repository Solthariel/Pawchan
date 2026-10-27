package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Separate public probe client: no CookieJar, credentials, redirects or routing interceptors. */
internal class MirrorProbe : AutoCloseable {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROBE_CALL_SECONDS, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()

    fun start(
        host: String,
        report: (MirrorObservation) -> Unit,
    ): Call {
        val call =
            client.newCall(
                Request
                    .Builder()
                    .url("https://$host/index.json")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build(),
            )
        call.enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    report(
                        MirrorObservation(
                            host,
                            if (e is java.io.InterruptedIOException && !call.isCanceled()) {
                                MirrorAvailability.TIMEOUT
                            } else {
                                MirrorAvailability.NETWORK_ERROR
                            },
                            System.currentTimeMillis(),
                        ),
                    )
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    val status = response.use(::classifyMirrorResponse)
                    report(MirrorObservation(host, status, System.currentTimeMillis()))
                }
            },
        )
        return call
    }

    override fun close() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

internal fun classifyMirrorResponse(response: Response): MirrorAvailability =
    when {
        response.code == RATE_LIMITED_STATUS -> MirrorAvailability.RATE_LIMITED
        response.code == FORBIDDEN_STATUS || response.code == UNAUTHORIZED_STATUS -> MirrorAvailability.VERIFICATION
        response.code >= SERVER_ERROR_STATUS -> MirrorAvailability.SERVER_ERROR
        response.code != SUCCESS_STATUS -> MirrorAvailability.INVALID_RESPONSE
        else -> mirrorJsonAvailability(response)
    }

private fun mirrorJsonAvailability(response: Response): MirrorAvailability =
    try {
        val body = response.body
        val source = body.source()
        source.request(MAX_MIRROR_PROBE_BYTES + 1L)
        if (source.buffer.size > MAX_MIRROR_PROBE_BYTES) {
            MirrorAvailability.INVALID_RESPONSE
        } else {
            val root = Json.parseToJsonElement(body.string()) as? JsonObject
            val boards = root?.get("boards") as? JsonArray
            val valid =
                !boards.isNullOrEmpty() &&
                    boards.all { row ->
                        val board = row as? JsonObject
                        (board?.get("id") as? JsonPrimitive)?.contentOrNull?.isNotBlank() == true &&
                            (board["name"] as? JsonPrimitive)?.contentOrNull?.isNotBlank() == true
                    }
            if (valid) MirrorAvailability.AVAILABLE else MirrorAvailability.INVALID_RESPONSE
        }
    } catch (_: SerializationException) {
        MirrorAvailability.INVALID_RESPONSE
    } catch (_: IllegalArgumentException) {
        MirrorAvailability.INVALID_RESPONSE
    } catch (_: IOException) {
        MirrorAvailability.NETWORK_ERROR
    }

private const val MAX_MIRROR_PROBE_BYTES = 1_048_576L

private const val PROBE_TIMEOUT_SECONDS = 3L
private const val PROBE_CALL_SECONDS = 4L
private const val FORBIDDEN_STATUS = 403
private const val UNAUTHORIZED_STATUS = 401
private const val SERVER_ERROR_STATUS = 500
private const val SUCCESS_STATUS = 200
