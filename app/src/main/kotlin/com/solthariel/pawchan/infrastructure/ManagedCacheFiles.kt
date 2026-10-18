package com.solthariel.pawchan.infrastructure

import java.io.File

/** Used only after network/image resources have been closed, on a background thread. */
internal fun clearClosedSessionCache(directory: File): Boolean {
    val managed = setOf("thumbnail_cache", "image_cache")
    return directory
        .listFiles()
        .orEmpty()
        .filter {
            it.name in managed
        }.map { it.deleteRecursively() }
        .all { it }
}
