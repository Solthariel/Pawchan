package com.solthariel.pawchan.media

/** Request identity is independent of whether a page is selected or adjacent. */
internal data class ViewerImagePageKey(
    val url: String,
    val retryToken: Int = 0,
)

internal fun viewerImagePageKeys(
    current: String,
    thumbnail: String?,
    neighbors: List<String>,
    retryToken: Int,
): List<ViewerImagePageKey> =
    buildList {
        add(ViewerImagePageKey(current, retryToken))
        thumbnail?.let { add(ViewerImagePageKey(it)) }
        neighbors.forEach { add(ViewerImagePageKey(it)) }
    }.distinctBy { it.url }
