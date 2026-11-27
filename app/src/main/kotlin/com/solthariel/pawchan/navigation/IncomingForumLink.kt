package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import java.net.URI
import java.net.URISyntaxException

/** Only explicit web URLs on known forum origins are accepted from other applications. */
internal fun incomingForumLink(url: String): ForumWebLink? {
    val uri = incomingUri(url) ?: return null
    val link =
        if (uri.userInfo == null) {
            when (uri.scheme?.lowercase()) {
                "https", "http" -> incomingWebLink(uri)
                "pawchan" -> incomingNotificationLink(uri)
                else -> null
            }
        } else {
            null
        }
    return link.takeIf { it is ForumWebLink.Board || it is ForumWebLink.Thread }
}

private fun incomingUri(url: String): URI? =
    try {
        URI(url)
    } catch (_: URISyntaxException) {
        null
    }

private fun incomingWebLink(uri: URI): ForumWebLink? {
    val defaultPort = if (uri.scheme.equals("http", true)) HTTP_PORT else HTTPS_PORT
    val portAllowed = uri.port == -1 || uri.port == defaultPort
    val host = uri.host
    if (!portAllowed || uri.rawPath != uri.path || host == null) return null
    val https = URI("https", null, host.lowercase(), -1, uri.path, uri.query, uri.fragment)
    return com.solthariel.pawchan.infrastructure.sites
        .appForumWebLinks()
        .firstNotNullOfOrNull { it.webLink(https) }
}

private fun incomingNotificationLink(uri: URI): ForumWebLink? {
    if (uri.host != "thread" || uri.port != -1) return null
    return AppRoute
        .threadAddress("thread${uri.path}")
        ?.takeIf { address ->
            com.solthariel.pawchan.infrastructure.sites
                .appForumWebLinks()
                .any { it.site == address.site }
        }?.let { ForumWebLink.Thread(it, null, replies = uri.rawQuery == "replies=1") }
}

private const val HTTP_PORT = 80
private const val HTTPS_PORT = 443
