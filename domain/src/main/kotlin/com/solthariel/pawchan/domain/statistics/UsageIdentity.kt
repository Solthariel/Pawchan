package com.solthariel.pawchan.domain.statistics

import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale

/** Same remote resource across supported Dvach mirrors; query parameters remain significant. */
fun usageMediaIdentity(url: String): String {
    val value = url.trim()
    return try {
        val uri = URI(value).normalize()
        val host = uri.host?.lowercase(Locale.ROOT)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (host == null || scheme == null) return value
        val mirror = host in USAGE_DVACH_HOSTS
        val canonicalHost = if (mirror) "2ch.su" else host
        val canonicalScheme = if (mirror) "https" else scheme
        val port =
            uri.port.takeUnless {
                it == -1 || (scheme == "https" && it == USAGE_HTTPS_PORT) || (scheme == "http" && it == USAGE_HTTP_PORT)
            }
        val authority = if (':' in canonicalHost) "[$canonicalHost]" else canonicalHost
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        "$canonicalScheme://$authority${port?.let { ":$it" }.orEmpty()}${uri.rawPath.orEmpty()}$query"
    } catch (_: URISyntaxException) {
        value
    }
}

/** Prepared attachment bytes, independent of post ID, filename, attachment order or retry. */
fun usageFileIdentity(content: ByteArray): String = "content:" + digestUsageIdentity(content)

fun usageObjectHash(identity: String): String = digestUsageIdentity(identity.toByteArray(Charsets.UTF_8))

private fun digestUsageIdentity(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private val USAGE_DVACH_HOSTS = setOf("2ch.su", "2ch.org", "2ch.life", "2ch.hk", "2ch.pm")

private const val USAGE_HTTPS_PORT = 443
private const val USAGE_HTTP_PORT = 80
