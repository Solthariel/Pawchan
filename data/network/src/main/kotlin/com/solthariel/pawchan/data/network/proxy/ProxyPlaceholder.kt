package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import java.net.InetAddress
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale

/**
 * A proxied resource is never resolved on this device: the proxy resolves it. OkHttp still wants
 * an address to connect to, so [RoutedDns] hands out a private-range placeholder that carries the
 * resource, the route generation and a digest of the host name, and the tunnel socket reads it back.
 * A placeholder that escapes to a real socket goes nowhere: the range is not routable.
 */
internal data class ProxyPlaceholder(
    val service: ProxyService,
    val epoch: Int,
    val hostDigest: Long,
)

internal fun placeholderAddress(
    service: ProxyService,
    epoch: Int,
    host: String,
): InetAddress {
    val bytes =
        ByteBuffer
            .allocate(PLACEHOLDER_BYTES)
            .put(MAGIC)
            .put(service.ordinal.toByte())
            .putInt(epoch)
            .putLong(hostDigest(host))
            .array()
    return InetAddress.getByAddress(host, bytes)
}

internal fun parsePlaceholder(address: InetAddress?): ProxyPlaceholder? {
    val bytes =
        address?.address?.takeIf { it.size == PLACEHOLDER_BYTES && it.copyOfRange(0, MAGIC.size).contentEquals(MAGIC) }
    val buffer = bytes?.let { ByteBuffer.wrap(it, MAGIC.size, PLACEHOLDER_BYTES - MAGIC.size) }
    val service = buffer?.get()?.toInt()?.let { ProxyService.entries.getOrNull(it) }
    return if (buffer != null && service != null) ProxyPlaceholder(service, buffer.getInt(), buffer.getLong()) else null
}

internal fun hostDigest(host: String): Long =
    ByteBuffer
        .wrap(MessageDigest.getInstance("SHA-256").digest(host.lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII)))
        .getLong()

// fd50:43xx::/32 lies in the unique-local range: it is never routed on the internet.
private const val MAGIC_ULA = 0xFD
private const val MAGIC_P = 0x50
private const val MAGIC_C = 0x43
private val MAGIC = byteArrayOf(MAGIC_ULA.toByte(), MAGIC_P.toByte(), MAGIC_C.toByte())
private const val PLACEHOLDER_BYTES = 16
