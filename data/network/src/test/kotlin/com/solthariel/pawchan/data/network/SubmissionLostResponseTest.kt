package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SubmissionLostResponseTest {
    @Test
    fun serverReceivesTheBodyButDropsTheResponseAndNoSecondBodyIsSent() =
        runBlocking {
            val writes = AtomicInteger()
            ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
                server.soTimeout = 5_000
                val executor = Executors.newSingleThreadExecutor()
                val accepted =
                    executor.submit<String> {
                        server.accept().use { socket ->
                            socket.soTimeout = 5_000
                            val input = socket.getInputStream().bufferedReader(Charsets.US_ASCII)
                            val headers = generateSequence { input.readLine() }.takeWhile { it.isNotEmpty() }.toList()
                            val length =
                                headers
                                    .first { it.startsWith("Content-Length:", true) }
                                    .substringAfter(':')
                                    .trim()
                                    .toInt()
                            val body = CharArray(length)
                            var offset = 0
                            while (offset < length) {
                                val read = input.read(body, offset, length - offset)
                                check(read > 0)
                                offset += read
                            }
                            String(body) // Closing without a status line simulates a lost response after acceptance.
                        }
                    }
                val client = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
                try {
                    val body = CountingSubmissionBody(writes)
                    val request =
                        Request
                            .Builder()
                            .url("http://127.0.0.1:${server.localPort}/post")
                            .post(body)
                            .build()
                    val outcome = executeSubmissionRequest(client, request) { PostingOutcome.Success("confirmed") }
                    assertEquals("payload", accepted.get(5, TimeUnit.SECONDS))
                    assertEquals(PostingOutcome.Failure(PostingFailure.SubmissionUnconfirmed), outcome)
                    assertEquals(1, writes.get())
                } finally {
                    client.dispatcher.executorService.shutdown()
                    client.connectionPool.evictAll()
                    executor.shutdownNow()
                }
            }
        }
}

private class CountingSubmissionBody(
    private val writes: AtomicInteger,
) : RequestBody() {
    override fun contentType(): MediaType? = null

    override fun contentLength() = 7L

    override fun writeTo(sink: BufferedSink) {
        writes.incrementAndGet()
        sink.writeUtf8("payload")
    }
}
