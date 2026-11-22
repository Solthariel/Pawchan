package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProxyPooledConnectionTest {
    @Test fun directConnectionInPoolCannotBypassNewProxyPolicy() {
        val server = ServerSocket(0)
        val requests = AtomicInteger()
        val executor = Executors.newSingleThreadExecutor()
        val job =
            executor.submit {
                server.accept().use { socket ->
                    socket.soTimeout = 1500
                    val input = socket.getInputStream().bufferedReader()
                    try {
                        while (readRequest(input)) {
                            requests.incrementAndGet()
                            val response =
                                "HTTP/1.1 200 OK\r\nContent-Length: 2\r\n" +
                                    "Connection: keep-alive\r\n\r\nOK"
                            socket.getOutputStream().write(response.toByteArray())
                            socket.getOutputStream().flush()
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            }
        val router = ScopedProxySelector()
        val client =
            OkHttpClient
                .Builder()
                .viaProxyRouter(router, directDns = Dns { listOf(InetAddress.getByName("127.0.0.1")) })
                .build()
        val request = Request.Builder().url("http://arhivach.vc:${server.localPort}/").build()
        try {
            client.newCall(request).execute().use { assertEquals("OK", it.body.string()) }
            router.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
            val failure = failureOf { client.newCall(request).execute().close() }
            assertEquals(ProxyFailure.NOT_READY, failure.proxyFailure())
            assertEquals(1, requests.get())
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
            server.close()
            job.get(3, TimeUnit.SECONDS)
            executor.shutdownNow()
        }
    }
}

private fun readRequest(input: BufferedReader): Boolean {
    var first = input.readLine()
    while (first != null && first.isEmpty()) {
        first = input.readLine()
    }
    if (first == null) return false
    while (!input.readLine().isNullOrEmpty()) { /* discard headers */ }
    return true
}
