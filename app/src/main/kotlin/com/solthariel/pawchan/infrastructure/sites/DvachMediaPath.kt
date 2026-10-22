package com.solthariel.pawchan.infrastructure.sites

import java.net.URI

/** Только оригиналы 2ch: доска, тред и полное имя остаются частью ключа. */
internal fun dvachMediaPath(uri: URI): String? {
    val match = DVACH_MEDIA_PATH.matchEntire(uri.rawPath.orEmpty()) ?: return null
    val (board, thread, file) = match.destructured
    return "/$board/src/$thread/$file"
}

private val DVACH_MEDIA_PATH =
    Regex("""/([a-z0-9]{1,16})/(?:arch/(?:[0-9]{4}-[0-9]{2}-[0-9]{2}/)?)?src/([0-9]+)/([^/%]+)""")
