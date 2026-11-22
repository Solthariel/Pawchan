package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/** A certificate from the test resources, usable as a server key and as the only trust anchor. */
internal class TestTls(
    val context: SSLContext,
    val trustManager: X509TrustManager,
)

internal fun testTls(resource: String): TestTls {
    val password = "testpass".toCharArray()
    val keystore =
        KeyStore.getInstance("PKCS12").apply {
            requireNotNull(TestTls::class.java.getResourceAsStream(resource)) { resource }.use { load(it, password) }
        }
    val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keystore, password) }
    val trusts = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keystore) }
    val context = SSLContext.getInstance("TLS").apply { init(keys.keyManagers, trusts.trustManagers, null) }
    return TestTls(context, trusts.trustManagers.filterIsInstance<X509TrustManager>().single())
}

/** arhivach.vc: the resource behind the proxy. */
internal val resourceTls by lazy { testTls("/proxy-test.p12") }

/** localhost, 127.0.0.1 and ::1: a correctly configured HTTPS proxy. */
internal val proxyTls by lazy { testTls("/proxy-host-test.p12") }

/** wrong.example: an HTTPS proxy whose certificate names another host. */
internal val wrongHostTls by lazy { testTls("/proxy-wrong-host-test.p12") }

internal fun bigPayload(size: Int = BIG_PAYLOAD_BYTES): ByteArray = ByteArray(size) { (it % PAYLOAD_PATTERN).toByte() }

/** The resource on a local port, over TLS as arhivach.vc. Counts every request that reaches it. */
internal class TestResource : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()
    val requests = AtomicInteger()
    val methods = CopyOnWriteArrayList<String>()
    private val server =
        HttpsServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            httpsConfigurator = HttpsConfigurator(resourceTls.context)
            setExecutor(executor)
            createContext("/") { exchange -> respond(exchange) }
            start()
        }
    val port: Int get() = server.address.port

    fun url(path: String = "/"): HttpUrl = "https://arhivach.vc:$port$path".toHttpUrl()

    private fun respond(exchange: HttpExchange) {
        requests.incrementAndGet()
        methods.add(exchange.requestMethod)
        val received = exchange.requestBody.use { it.readBytes() }
        val payload =
            when (exchange.requestURI.path) {
                "/big" -> bigPayload()
                "/echo" -> echoOf(received).toByteArray()
                else -> RESOURCE_BODY.toByteArray()
            }
        exchange.sendResponseHeaders(HTTP_OK, payload.size.toLong())
        exchange.responseBody.use { it.write(payload) }
    }

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }
}

internal fun echoOf(body: ByteArray): String = "${body.size}:${body.contentHashCode()}"

internal enum class TestProxyKind { SOCKS5, HTTP, HTTPS, NOT_A_PROXY }

/**
 * A local proxy that really tunnels. It parses the handshake, checks the login when one is set,
 * records what it was asked for and pumps bytes to [targetPort]: it "resolves" every resource name
 * to the local resource, the way a real proxy resolves names on its side.
 */
internal class TestProxy(
    private val kind: TestProxyKind,
    private val targetPort: Int,
    private val credentials: ProxyCredentials? = null,
    private val tls: TestTls = proxyTls,
    private val stall: Boolean = false,
) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()
    private val sockets = CopyOnWriteArrayList<Socket>()
    private val listener: ServerSocket =
        if (kind == TestProxyKind.HTTPS) {
            tls.context.serverSocketFactory.createServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())
        } else {
            ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())
        }
    val connections = AtomicInteger()
    val targets = CopyOnWriteArrayList<String>()
    val logins = CopyOnWriteArrayList<String>()
    val accepted = CountDownLatch(1)
    val disconnected = CountDownLatch(1)
    val endpoint: ProxyEndpoint get() = ProxyEndpoint("127.0.0.1", listener.localPort)

    init {
        executor.execute(::acceptLoop)
    }

    private fun acceptLoop() {
        while (!listener.isClosed) {
            val client =
                try {
                    listener.accept()
                } catch (_: IOException) {
                    break
                }
            sockets.add(client)
            connections.incrementAndGet()
            accepted.countDown()
            executor.execute { serve(client) }
        }
    }

    private fun serve(client: Socket) {
        try {
            client.soTimeout = SERVER_TIMEOUT_MILLIS
            when {
                kind == TestProxyKind.NOT_A_PROXY -> writeAll(client.getOutputStream(), NOT_A_PROXY_REPLY)
                stall -> client.getInputStream().copyTo(OutputStream.nullOutputStream())
                else -> handshake(client.getInputStream(), client.getOutputStream())?.let { tunnel(client, it) }
            }
        } catch (_: IOException) {
            // The client went away or failed its handshake: nothing to serve.
        } finally {
            disconnected.countDown()
            closeQuietly(client)
        }
    }

    /** Returns the requested `host:port`, or null after refusing the client. */
    private fun handshake(
        input: InputStream,
        output: OutputStream,
    ): String? =
        if (kind == TestProxyKind.SOCKS5) {
            TestSocksServer(credentials, logins).handshake(input, output)
        } else {
            connectHandshake(input, output)
        }

    private fun connectHandshake(
        input: InputStream,
        output: OutputStream,
    ): String? {
        val lines = readHead(input).split("\r\n")
        val authorization =
            lines.firstOrNull { it.startsWith("Proxy-Authorization:", ignoreCase = true) }?.substringAfter(':')?.trim()
        authorization?.let(logins::add)
        val expected = credentials?.let { "Basic " + basic(it) }
        if (expected != null && authorization != expected) {
            writeAll(output, PROXY_AUTH_REQUIRED_REPLY)
            return null
        }
        writeAll(output, "HTTP/1.1 200 Connection established\r\n\r\n")
        return lines.first().removePrefix("CONNECT ").substringBefore(' ')
    }

    private fun tunnel(
        client: Socket,
        target: String,
    ) {
        targets.add(target)
        val upstream = Socket(InetAddress.getLoopbackAddress(), targetPort)
        sockets.add(upstream)
        executor.execute {
            pumpQuietly(upstream.getInputStream(), client.getOutputStream())
            closeQuietly(client)
        }
        pumpQuietly(client.getInputStream(), upstream.getOutputStream())
        closeQuietly(upstream)
    }

    override fun close() {
        listener.close()
        sockets.forEach(::closeQuietly)
        executor.shutdownNow()
    }
}

/** The SOCKS5 side of [TestProxy]: no-auth or RFC 1929 login, CONNECT by host name only. */
private class TestSocksServer(
    private val credentials: ProxyCredentials?,
    private val logins: MutableList<String>,
) {
    fun handshake(
        input: InputStream,
        output: OutputStream,
    ): String? {
        if (input.read() != SOCKS_VERSION) throw IOException("Not SOCKS5")
        val offered = input.readNBytes(input.read()).map { it.toInt() }
        val method = if (credentials == null) SOCKS_NO_AUTH else SOCKS_PASSWORD
        val accepted = method in offered
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), (if (accepted) method else SOCKS_NO_METHOD).toByte()))
        output.flush()
        val loggedIn = accepted && (credentials == null || login(input, output))
        return if (loggedIn) connect(input, output) else null
    }

    private fun login(
        input: InputStream,
        output: OutputStream,
    ): Boolean {
        if (input.read() != 1) throw IOException("Not RFC 1929")
        val user = String(input.readNBytes(input.read()), Charsets.UTF_8)
        val password = String(input.readNBytes(input.read()), Charsets.UTF_8)
        logins.add("$user:$password")
        val valid = ProxyCredentials(user, password) == credentials
        output.write(byteArrayOf(1, if (valid) 0 else 1))
        output.flush()
        return valid
    }

    private fun connect(
        input: InputStream,
        output: OutputStream,
    ): String {
        val header = input.readNBytes(SOCKS_HEADER_BYTES)
        if (header[1].toInt() != 1 || header[SOCKS_HEADER_BYTES - 1].toInt() != SOCKS_DOMAIN) {
            throw IOException("Only CONNECT by host name is expected")
        }
        val host = String(input.readNBytes(input.read()), Charsets.US_ASCII)
        val port = (input.read() shl Byte.SIZE_BITS) or input.read()
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), 0, 0, 1, 127, 0, 0, 1, 0, 1))
        output.flush()
        return "$host:$port"
    }
}

internal fun basic(credentials: ProxyCredentials): String =
    Base64.getEncoder().encodeToString("${credentials.username}:${credentials.password}".toByteArray(Charsets.UTF_8))

private fun readHead(input: InputStream): String {
    val head = StringBuilder()
    while (!head.endsWith("\r\n\r\n")) {
        val next = input.read()
        if (next < 0 || head.length > MAX_HEAD_LENGTH) throw IOException("Incomplete request head")
        head.append(next.toChar())
    }
    return head.toString()
}

private fun writeAll(
    output: OutputStream,
    text: String,
) {
    output.write(text.toByteArray(Charsets.ISO_8859_1))
    output.flush()
}

private fun pumpQuietly(
    from: InputStream,
    to: OutputStream,
) {
    val buffer = ByteArray(PUMP_BYTES)
    try {
        while (true) {
            val read = from.read(buffer)
            if (read < 0) break
            to.write(buffer, 0, read)
            to.flush()
        }
    } catch (_: IOException) {
        // One side closed: the tunnel is over.
    }
}

internal const val RESOURCE_BODY = "through-proxy"
internal const val BIG_PAYLOAD_BYTES = 3 * 1024 * 1024 + 17
private const val PAYLOAD_PATTERN = 251
private const val HTTP_OK = 200
private const val BACKLOG = 50
private const val SERVER_TIMEOUT_MILLIS = 10_000
private const val PUMP_BYTES = 16 * 1024
private const val MAX_HEAD_LENGTH = 16 * 1024
private const val SOCKS_VERSION = 5
private const val SOCKS_NO_AUTH = 0
private const val SOCKS_PASSWORD = 2
private const val SOCKS_NO_METHOD = 0xFF
private const val SOCKS_DOMAIN = 3
private const val SOCKS_HEADER_BYTES = 4
private const val NOT_A_PROXY_REPLY = "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n"
private const val PROXY_AUTH_REQUIRED_REPLY =
    "HTTP/1.1 407 Proxy Authentication Required\r\n" +
        "Proxy-Authenticate: Basic realm=\"test\"\r\nContent-Length: 0\r\n\r\n"
