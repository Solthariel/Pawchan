package com.solthariel.pawchan.media.thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.FastScrollGesture

data class ThumbnailLoading(
    val thumbnailLoader: ThumbnailLoader,
    val gesture: FastScrollGesture,
)

@Composable
fun rememberThumbnailLoading(
    loader: ThumbnailLoader,
    state: LazyListState,
): ThumbnailLoading {
    val gesture = remember(state) { FastScrollGesture() }
    return remember(loader, gesture) { ThumbnailLoading(loader, gesture) }
}

@Composable
fun DeferredThumbnail(
    thumbnailUrl: String?,
    loading: ThumbnailLoading,
    contentDescription: String?,
    modifier: Modifier,
) {
    if (thumbnailUrl == null) return
    val subscription =
        remember(loading, thumbnailUrl) {
            loading.thumbnailLoader.subscription(thumbnailUrl)
        }
    val bitmap by subscription.bitmap.collectAsState()
    val failed by subscription.failed.collectAsState()
    val loaded = bitmap
    if (loaded == null) {
        ThumbnailPlaceholder(failed, contentDescription, modifier)
    } else {
        FadingThumbnail(loaded, subscription.instantly, contentDescription, modifier)
    }
}

@Composable
private fun ThumbnailPlaceholder(
    failed: Boolean,
    description: String?,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(if (failed) R.drawable.thumbnail_unavailable else R.drawable.thumbnail_pending),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PLACEHOLDER_ALPHA),
        )
    }
}

private const val PLACEHOLDER_ALPHA = 0.45f
