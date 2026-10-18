package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest

/** Paging and retry use the same cache identity; retry bypasses reads, not future hits. */
@Composable
internal fun rememberMediaImageRequest(
    url: String?,
    retryToken: Int = 0,
): ImageRequest {
    val context = LocalContext.current
    return remember(context, url, retryToken) {
        ImageRequest
            .Builder(context)
            .data(url)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .placeholderMemoryCacheKey(url)
            .memoryCachePolicy(if (retryToken == 0) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY)
            .diskCachePolicy(if (retryToken == 0) CachePolicy.ENABLED else CachePolicy.WRITE_ONLY)
            .build()
    }
}
