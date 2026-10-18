package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

data class MediaPresentation(
    val localUrls: Map<String, String> = emptyMap(),
    val backHighlighted: Boolean = false,
)

val LocalMediaPresentation = compositionLocalOf { MediaPresentation() }

@Composable
internal fun resolvedMediaUrl(url: String): String {
    val localUrl = LocalMediaPresentation.current.localUrls[url]
    return localUrl?.takeIf { it.isNotEmpty() } ?: url
}
