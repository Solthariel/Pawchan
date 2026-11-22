package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Whatever goes wrong, a proxied resource is never reached directly and never resolved on the device. */
class ProxyFailClosedTest {
    private val bed = ProxyTestBed()

    @After fun tearDown() = bed.close()

    @Test fun unreachableProxyFailsWithoutAnyDirectRequest() {
        val closedPort = ServerSocket(0).use { it.localPort }
        val endpoint = ProxyEndpoint("127.0.0.1", closedPort)
        listOf(ProxyRoute.Socks5(endpoint), ProxyRoute.Http(endpoint), ProxyRoute.Https(endpoint)).forEach { route ->
            val failure = failureOf { bed.client(route).get(bed.resource.url()) }
            assertEquals("$route", ProxyFailure.PROXY_UNREACHABLE, failure.proxyFailure())
        }
        assertEquals(emptyList<String>(), bed.dns.lookups)
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun unavailableRouteIsRefusedBeforeAnyLookup() {
        val failure = failureOf { bed.client(ProxyRoute.Unavailable).get(bed.resource.url()) }
        assertEquals(ProxyFailure.NOT_READY, failure.proxyFailure())
        assertEquals(emptyList<String>(), bed.dns.lookups)
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun clientWithoutTheTunnelCannotReachAProxiedResource() {
        val proxy = bed.proxy()
        val router = routerWith(ProxyRoute.Socks5(proxy.endpoint))
        val forgotten =
            bed.adopt(
                OkHttpClient
                    .Builder()
                    .proxySelector(router)
                    .dns(bed.dns)
                    .sslSocketFactory(resourceTls.context.socketFactory, resourceTls.trustManager)
                    .callTimeout(5, TimeUnit.SECONDS)
                    .build(),
            )
        failureOf { forgotten.get(bed.resource.url()) }
        assertEquals(0, bed.resource.requests.get())
        assertEquals(0, proxy.connections.get())
        assertEquals(emptyList<String>(), bed.dns.lookups)
    }

    @Test fun cancellingAStalledHandshakeClosesTheProxyConnection() {
        val proxy = bed.proxy(stall = true)
        val client = bed.client(ProxyRoute.Socks5(proxy.endpoint))
        val call = client.newCall(Request.Builder().url(bed.resource.url()).build())
        val failed = CountDownLatch(1)
        call.enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) = failed.countDown()

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) = response.close()
            },
        )
        assertTrue(proxy.accepted.await(5, TimeUnit.SECONDS))
        call.cancel()
        assertTrue("The proxy connection must be closed", proxy.disconnected.await(5, TimeUnit.SECONDS))
        assertTrue("The call must fail", failed.await(5, TimeUnit.SECONDS))
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun routeChangeReplacesAPooledTunnelForGet() {
        val first = bed.proxy()
        val second = bed.proxy()
        val router = routerWith(ProxyRoute.Socks5(first.endpoint))
        val client = bed.client(router)
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(second.endpoint))
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        assertEquals(1, first.connections.get())
        assertEquals(1, second.targets.size)
        assertEquals(2, bed.resource.requests.get())
    }

    @Test fun routeChangeNeverResendsAPost() {
        val first = bed.proxy()
        val second = bed.proxy()
        val router = routerWith(ProxyRoute.Socks5(first.endpoint))
        val client = bed.client(router)
        val body = "reply".toByteArray()
        assertEquals(echoOf(body), client.post(bed.resource.url("/echo"), body))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(second.endpoint))
        val stale = failureOf { client.post(bed.resource.url("/echo"), body) }
        assertEquals(ProxyFailure.NOT_READY, stale.proxyFailure())
        assertEquals(1, bed.resource.requests.get())
        assertEquals(echoOf(body), client.post(bed.resource.url("/echo"), body))
        assertEquals(listOf("POST", "POST"), bed.resource.methods)
        assertEquals(1, second.targets.size)
    }

    @Test fun switchingToDirectDropsTheTunnel() {
        val proxy = bed.proxy()
        val router = routerWith(ProxyRoute.Socks5(proxy.endpoint))
        val client = bed.client(router)
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Direct)
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        assertEquals(1, proxy.targets.size)
        assertEquals(listOf("arhivach.vc"), bed.dns.lookups)
        assertEquals(2, bed.resource.requests.get())
    }

    @Test fun switchingToAProxyNeverReusesTheDirectConnection() {
        val proxy = bed.proxy()
        val router = ScopedProxySelector(null)
        val client = bed.client(router)
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(proxy.endpoint))
        assertEquals(RESOURCE_BODY, client.get(bed.resource.url()))
        assertEquals(listOf("arhivach.vc:${bed.resource.port}"), proxy.targets)
        assertEquals(listOf("arhivach.vc"), bed.dns.lookups)
        assertEquals(2, bed.resource.requests.get())
    }
}
