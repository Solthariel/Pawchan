package com.solthariel.pawchan.data.network.proxy

import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** HTTP CONNECT inside TLS to a local proxy whose certificate is checked like any other. */
class HttpsProxyEndToEndTest {
    private val bed = ProxyTestBed()

    @After fun tearDown() = bed.close()

    @Test fun verifiedProxyTunnelsTheResource() {
        val proxy = bed.proxy(TestProxyKind.HTTPS)
        assertEquals(RESOURCE_BODY, bed.client(ProxyRoute.Https(proxy.endpoint)).get(bed.resource.url()))
        assertEquals(listOf("arhivach.vc:${bed.resource.port}"), proxy.targets)
        assertEquals(emptyList<String>(), bed.dns.lookups)
        assertEquals(1, bed.resource.requests.get())
    }

    @Test fun loginTravelsInsideTheProxyTls() {
        val proxy = bed.proxy(TestProxyKind.HTTPS, LOGIN)
        assertEquals(RESOURCE_BODY, bed.client(ProxyRoute.Https(proxy.endpoint, LOGIN)).get(bed.resource.url()))
        assertEquals(listOf("Basic $LOGIN_BASIC"), proxy.logins)
        val wrong = failureOf { bed.client(ProxyRoute.Https(proxy.endpoint, WRONG_LOGIN)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.AUTH_REJECTED, wrong.proxyFailure())
        assertNoSecret(wrong, WRONG_LOGIN.password, WRONG_LOGIN_BASIC)
        assertEquals(1, bed.resource.requests.get())
    }

    @Test fun largeBodiesCrossTheRelayInBothDirections() {
        val proxy = bed.proxy(TestProxyKind.HTTPS)
        val client = bed.client(ProxyRoute.Https(proxy.endpoint))
        val download = client.newCall(Request.Builder().url(bed.resource.url("/big")).build()).execute()
        assertArrayEquals(bigPayload(), download.use { it.body.bytes() })
        val upload = bigPayload(UPLOAD_BYTES)
        assertEquals(echoOf(upload), client.post(bed.resource.url("/echo"), upload))
        assertEquals(listOf("GET", "POST"), bed.resource.methods)
    }

    @Test fun untrustedCertificateStopsTheTunnelBeforeTheLogin() {
        val proxy = bed.proxy(TestProxyKind.HTTPS, LOGIN)
        val client = bed.client(ProxyRoute.Https(proxy.endpoint, LOGIN), trusting(resourceTls))
        val failure = failureOf { client.get(bed.resource.url()) }
        assertEquals(ProxyFailure.TLS_UNTRUSTED, failure.proxyFailure())
        assertEquals(emptyList<String>(), proxy.logins)
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun certificateForAnotherHostIsRejected() {
        val proxy = bed.proxy(TestProxyKind.HTTPS, LOGIN, wrongHostTls)
        val client = bed.client(ProxyRoute.Https(proxy.endpoint, LOGIN), trusting(wrongHostTls))
        val failure = failureOf { client.get(bed.resource.url()) }
        assertEquals(ProxyFailure.TLS_HOSTNAME, failure.proxyFailure())
        assertEquals(emptyList<String>(), proxy.logins)
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun plainServerOnTheProxyPortIsATlsFailure() {
        val server = bed.proxy(TestProxyKind.NOT_A_PROXY)
        val failure = failureOf { bed.client(ProxyRoute.Https(server.endpoint)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.TLS_FAILED, failure.proxyFailure())
        assertEquals(0, bed.resource.requests.get())
    }

    /** What a plain server's reset looks like on Windows, made deterministic on every system. */
    @Test fun resetDuringTheTlsHandshakeIsATlsFailure() {
        val server = bed.proxy(TestProxyKind.RESETS_CONNECTION)
        val failure = failureOf { bed.client(ProxyRoute.Https(server.endpoint, LOGIN)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.TLS_FAILED, failure.proxyFailure())
        assertNoSecret(failure, LOGIN.password, LOGIN_BASIC)
        assertEquals(0, bed.resource.requests.get())
    }
}

private const val UPLOAD_BYTES = 1024 * 1024 + 3
