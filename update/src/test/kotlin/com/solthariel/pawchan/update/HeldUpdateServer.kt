package com.solthariel.pawchan.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Loopback only: holds one HTTP response, so tests cancel real socket reads without public networking. */
internal class HeldUpdateServer(
    headers: Boolean,
) : AutoCloseable {
    private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
    private val executor = Executors.newSingleThreadExecutor()
    private val request = CountDownLatch(1)
    private val disconnected = CountDownLatch(1)

    @Volatile private var socket: Socket? = null
    val url: HttpUrl = "http://127.0.0.1:${server.localPort}/apk".toHttpUrl()
    private val task =
        executor.submit {
            server.accept().use { connection ->
                socket = connection
                connection.soTimeout = 5000
                val input = connection.getInputStream().bufferedReader()
                while (!input.readLine().isNullOrEmpty()) { /* Consume request headers. */ }
                if (headers) {
                    connection.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Length: 6\r\nConnection: close\r\n\r\nab".toByteArray())
                        flush()
                    }
                }
                request.countDown()
                check(input.read() == -1) { "Client did not disconnect" }
                disconnected.countDown()
            }
        }

    suspend fun awaitRequest() =
        withContext(Dispatchers.IO) {
            check(request.await(5, TimeUnit.SECONDS)) { "No HTTP request received" }
        }

    suspend fun awaitDisconnect() =
        withContext(Dispatchers.IO) {
            check(disconnected.await(5, TimeUnit.SECONDS)) { "HTTP socket was not cancelled" }
            task.get(5, TimeUnit.SECONDS)
            Unit
        }

    override fun close() {
        socket?.close()
        server.close()
        executor.shutdownNow()
        check(executor.awaitTermination(5, TimeUnit.SECONDS))
    }
}
