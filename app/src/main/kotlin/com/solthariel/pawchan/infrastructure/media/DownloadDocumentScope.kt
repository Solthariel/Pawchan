package com.solthariel.pawchan.infrastructure.media

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class DownloadDocumentScope(
    val authority: String,
    val document: String,
)

/** Compare document IDs, not URI string prefixes (Old must not match Old2). */
internal fun downloadDocumentScope(url: String): DownloadDocumentScope? =
    try {
        val uri = URI(url)
        val parts = uri.rawPath.orEmpty().split('/')
        val index = parts.indexOf("document").takeIf { it >= 0 } ?: parts.indexOf("tree")
        val id = parts.getOrNull(index + 1)?.takeIf { index >= 0 && it.isNotEmpty() }
        if (uri.scheme == "content" && uri.authority != null && id != null) {
            DownloadDocumentScope(
                uri.authority,
                URLDecoder.decode(id.replace("+", "%2B"), StandardCharsets.UTF_8.name()),
            )
        } else {
            null
        }
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

internal fun downloadGrantCovers(
    tree: String,
    destination: String,
): Boolean {
    val parent = downloadDocumentScope(tree)
    val child = downloadDocumentScope(destination)
    return if (parent != null && child != null) {
        val related = child.document == parent.document || child.document.startsWith("${parent.document}/")
        parent.authority == child.authority && related
    } else {
        false
    }
}

internal fun canReleaseDownloadGrant(
    retired: String,
    selected: String?,
    destinations: List<String?>,
    leased: Set<String>,
): Boolean {
    val protected = retired == selected || leased.any { downloadGrantCovers(retired, it) }
    val unresolved = destinations.any { it == null || downloadDocumentScope(it) == null }
    return !protected && !unresolved && destinations.filterNotNull().none { downloadGrantCovers(retired, it) }
}
