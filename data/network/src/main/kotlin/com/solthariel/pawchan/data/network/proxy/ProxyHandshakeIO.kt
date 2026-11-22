package com.solthariel.pawchan.data.network.proxy

import java.io.InputStream

internal fun readLine(input: InputStream): String {
    val line = StringBuilder()
    while (true) {
        val next = input.read()
        ensure(next >= 0, ProxyFailure.BAD_PROXY_RESPONSE) { "The proxy closed the connection during the handshake" }
        if (next == '\n'.code) return line.toString().trimEnd('\r')
        line.append(next.toChar())
        ensure(line.length <= MAX_LINE_LENGTH, ProxyFailure.BAD_PROXY_RESPONSE) {
            "The proxy response line is too long"
        }
    }
}

internal fun readFully(
    input: InputStream,
    count: Int,
): ByteArray {
    val buffer = ByteArray(count)
    var offset = 0
    while (offset < count) {
        val read = input.read(buffer, offset, count - offset)
        ensure(read >= 0, ProxyFailure.BAD_PROXY_RESPONSE) { "The proxy closed the connection during the handshake" }
        offset += read
    }
    return buffer
}

internal inline fun ensure(
    condition: Boolean,
    failure: ProxyFailure,
    message: () -> String,
) {
    if (!condition) throw tunnelError(failure, message())
}

internal fun tunnelError(
    failure: ProxyFailure,
    message: String,
) = ProxyTunnelException(failure, message)

private const val MAX_LINE_LENGTH = 8192
