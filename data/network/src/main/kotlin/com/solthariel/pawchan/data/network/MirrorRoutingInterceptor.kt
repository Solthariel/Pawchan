package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.settings.validMirrorHost
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/** App-stack-owned routing. CookieJar runs afterwards against the destination origin. */
class MirrorRoutingInterceptor(
    initialHost: String = "2ch.su",
    val recovery: MirrorRecovery? = null,
) : Interceptor {
    @Volatile var host: String = initialHost
        set(value) {
            require(validMirrorHost(value))
            field = value
        }

    init {
        require(validMirrorHost(initialHost))
    }

    override fun intercept(chain: Interceptor.Chain): Response = readWithFallback(chain)

    fun route(request: okhttp3.Request): okhttp3.Request {
        val selected = recovery?.state?.value?.activeHost ?: host
        val original = request.url
        if (original.host !in SOURCE_HOSTS) return request
        val builder = request.newBuilder().url(original.newBuilder().host(selected).build())
        if (original.host != selected) {
            builder.removeHeader("Cookie").removeHeader("Authorization")
        }
        listOf("Origin", "Referer").forEach { name ->
            request.header(name)?.toHttpUrlOrNull()?.takeIf { it.host in SOURCE_HOSTS }?.let { url ->
                builder.header(
                    name,
                    url
                        .newBuilder()
                        .host(selected)
                        .build()
                        .toString()
                        .trimEnd('/'),
                )
            }
        }
        return builder.build()
    }
}

private val SOURCE_HOSTS = setOf("2ch.su", "2ch.org", "2ch.life", "2ch.hk", "2ch.pm")
