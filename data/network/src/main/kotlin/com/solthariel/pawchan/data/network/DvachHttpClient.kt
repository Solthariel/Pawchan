package com.solthariel.pawchan.data.network

import okhttp3.ConnectionSpec
import okhttp3.CookieJar
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.time.Duration

object DvachHttpClient {
    fun create(cookieJar: CookieJar = CookieJar.NO_COOKIES): OkHttpClient =
        OkHttpClient
            .Builder()
            .cookieJar(cookieJar)
            .addInterceptor(ServerBackoffInterceptor())
            .connectTimeout(CONNECT_TIMEOUT)
            .readTimeout(READ_TIMEOUT)
            .callTimeout(CALL_TIMEOUT)
            .followRedirects(false)
            .followSslRedirects(false)
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()

    fun createContentClient(apiClient: OkHttpClient): OkHttpClient =
        apiClient
            .newBuilder()
            .dispatcher(contentDispatcher())
            .followRedirects(true)
            .followSslRedirects(false)
            .callTimeout(Duration.ZERO)
            .build()

    private fun contentDispatcher(): Dispatcher =
        Dispatcher().apply {
            maxRequests = CONTENT_REQUEST_LIMIT
            maxRequestsPerHost = CONTENT_REQUEST_LIMIT
        }
}

internal val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS)
internal val READ_TIMEOUT: Duration = Duration.ofSeconds(READ_TIMEOUT_SECONDS)
internal val CALL_TIMEOUT: Duration = Duration.ofSeconds(CALL_TIMEOUT_SECONDS)

private const val CONNECT_TIMEOUT_SECONDS = 10L
private const val READ_TIMEOUT_SECONDS = 20L
private const val CALL_TIMEOUT_SECONDS = 30L
internal const val CONTENT_REQUEST_LIMIT = 3
