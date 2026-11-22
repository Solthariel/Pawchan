package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ServerSocket

class ProxyResourceProbeTest {
    @Test fun refusesDirectUnknownAndCleartextTargets() =
        runBlocking {
            val router = ScopedProxySelector(null)
            assertEquals(
                ProxyProbeResult.Unavailable,
                probeTarget(router, "https://arhivach.vc/".toHttpUrl(), OkHttpClient.Builder()),
            )
            router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)))
            assertEquals(
                ProxyProbeResult.Unavailable,
                probeTarget(router, "http://arhivach.vc/".toHttpUrl(), OkHttpClient.Builder()),
            )
            assertEquals(
                ProxyProbeResult.Unavailable,
                probeTarget(router, "https://example.com/".toHttpUrl(), OkHttpClient.Builder()),
            )
        }

    @Test fun unavailableProxyReturnsFailureNotDirectFallback() =
        runBlocking {
            val port = ServerSocket(0).use { it.localPort }
            val router = ScopedProxySelector(null)
            val endpoint = ProxyEndpoint("127.0.0.1", port)
            val routes = listOf(ProxyRoute.Socks5(endpoint), ProxyRoute.Http(endpoint), ProxyRoute.Https(endpoint))
            routes.forEach { route ->
                router.update(ProxyService.ARCHIVE, route)
                assertEquals(
                    ProxyProbeResult.Failed(ProxyFailure.PROXY_UNREACHABLE),
                    probeTarget(router, "https://arhivach.vc/".toHttpUrl(), OkHttpClient.Builder()),
                )
            }
        }

    @Test fun cancelledProbeClosesItsInFlightCall() =
        runBlocking {
            val listener = ServerSocket(0)
            val accepted = CompletableDeferred<Unit>()
            val disconnected = CompletableDeferred<Unit>()
            val server =
                launch(Dispatchers.IO) {
                    listener.accept().use { socket ->
                        socket.soTimeout = 3000
                        accepted.complete(Unit)
                        socket.getInputStream().copyTo(java.io.OutputStream.nullOutputStream())
                        disconnected.complete(Unit)
                    }
                }
            val router = ScopedProxySelector(null)
            router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", listener.localPort)))
            val job =
                launch {
                    probeTarget(router, "https://arhivach.vc/".toHttpUrl(), OkHttpClient.Builder())
                }
            try {
                withTimeout(3000) { accepted.await() }
                job.cancelAndJoin()
                withTimeout(3000) { disconnected.await() }
            } finally {
                job.cancelAndJoin()
                listener.close()
                withContext(Dispatchers.IO) { server.cancelAndJoin() }
            }
        }

    @Test fun resourceCheckRequiresConfiguredProxyAndValidMirror() =
        runBlocking {
            val router = ScopedProxySelector(null)
            val checker = ProxyResourceProbe(router)
            assertEquals(ProxyProbeResult.Unavailable, checker.check(ProxyService.ARCHIVE, "2ch.su"))
            router.update(ProxyService.DVACH, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)))
            assertEquals(ProxyProbeResult.Unavailable, checker.check(ProxyService.DVACH, "bad/host"))
            assertEquals(ProxyProbeResult.Unavailable, checker.check(ProxyService.DVACH, "github.com"))
        }
}
