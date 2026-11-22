package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyService
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertFalse
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/** Login with non-ASCII characters and a colon in the password: both must survive every protocol. */
internal val LOGIN = ProxyCredentials("алиса", "pässw:rd")
internal val WRONG_LOGIN = ProxyCredentials("алиса", "wrong-secret")

/** `Basic` token of [LOGIN], computed independently of the code under test. */
internal const val LOGIN_BASIC = "0LDQu9C40YHQsDpww6Rzc3c6cmQ="
internal const val WRONG_LOGIN_BASIC = "0LDQu9C40YHQsDp3cm9uZy1zZWNyZXQ="

/**
 * Resolves every name to this machine, so a direct connection would really reach the local
 * resource and be counted there. Records each lookup: a proxied resource must never appear.
 */
internal class DirectDnsSpy : Dns {
    val lookups = CopyOnWriteArrayList<String>()

    override fun lookup(hostname: String): List<InetAddress> {
        lookups.add(hostname)
        return listOf(InetAddress.getLoopbackAddress())
    }
}

/** The HTTPS proxy is judged by [anchor] with strict host name matching, as the system trust is in the app. */
internal fun trusting(anchor: TestTls): ProxyTlsTrust {
    val socketFactory = anchor.context.socketFactory
    return ProxyTlsTrust(socketFactory, OkHttpClient().hostnameVerifier)
}

internal fun routerWith(
    route: ProxyRoute,
    service: ProxyService = ProxyService.ARCHIVE,
): ScopedProxySelector = ScopedProxySelector(null).apply { update(service, route) }

/** One local resource, the proxies in front of it and production-shaped clients; all closed after the test. */
internal class ProxyTestBed : AutoCloseable {
    val resource = TestResource()
    val dns = DirectDnsSpy()
    private val opened = mutableListOf<AutoCloseable>()

    fun proxy(
        kind: TestProxyKind = TestProxyKind.SOCKS5,
        credentials: ProxyCredentials? = null,
        tls: TestTls = proxyTls,
        stall: Boolean = false,
    ): TestProxy = TestProxy(kind, resource.port, credentials, tls, stall).also(opened::add)

    /** Configured exactly like the app's clients: [viaProxyRouter], plus trust in the test resource. */
    fun client(
        router: ScopedProxySelector,
        trust: ProxyTlsTrust = trusting(proxyTls),
    ): OkHttpClient =
        adopt(
            OkHttpClient
                .Builder()
                .viaProxyRouter(router, trust, dns)
                .sslSocketFactory(resourceTls.context.socketFactory, resourceTls.trustManager)
                .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build(),
        )

    fun client(
        route: ProxyRoute,
        trust: ProxyTlsTrust = trusting(proxyTls),
    ): OkHttpClient = client(routerWith(route), trust)

    fun adopt(client: OkHttpClient): OkHttpClient =
        client.also {
            opened.add(
                AutoCloseable {
                    it.dispatcher.cancelAll()
                    it.connectionPool.evictAll()
                    it.dispatcher.executorService.shutdown()
                },
            )
        }

    override fun close() {
        opened.asReversed().forEach { it.close() }
        resource.close()
    }
}

internal fun OkHttpClient.get(url: HttpUrl): String {
    val request = Request.Builder().url(url).build()
    return newCall(request).execute().use { it.body.string() }
}

internal fun OkHttpClient.post(
    url: HttpUrl,
    body: ByteArray,
): String {
    val request =
        Request
            .Builder()
            .url(url)
            .post(body.toRequestBody(BINARY))
            .build()
    return newCall(request).execute().use { it.body.string() }
}

/** Runs [request], which must fail, and returns the failure. */
internal fun failureOf(request: () -> Unit): IOException =
    try {
        request()
        throw AssertionError("The request was expected to fail")
    } catch (failure: IOException) {
        failure
    }

/** No message, cause or suppressed exception may reveal a secret. */
internal fun assertNoSecret(
    failure: Throwable,
    vararg secrets: String,
) {
    val texts =
        generateSequence(failure) { it.cause }
            .flatMap { sequenceOf(it) + it.suppressed.asSequence() }
            .map { it.toString() }
            .toList()
    secrets.forEach { secret -> texts.forEach { assertFalse(it, secret in it) } }
}

private val BINARY = "application/octet-stream".toMediaType()
private const val CALL_TIMEOUT_SECONDS = 15L
