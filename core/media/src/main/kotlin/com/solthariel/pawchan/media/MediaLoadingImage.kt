package com.solthariel.pawchan.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.media.thumbnail.LocalThumbnailLoader

/** Cached thread thumbnail first, Coil second; an optional reserve stays until a real image is ready. */
@Composable
internal fun MediaLoadingImage(
    url: String?,
    loader: ImageLoader,
    modifier: Modifier = Modifier,
    onState: ((String) -> Unit)? = null,
    fallback: @Composable () -> Unit = {},
) {
    // Both subscription collectors and Coil's readiness belong to this URL, never the last item.
    key(url) { LoadingImageContent(url, loader, modifier, onState, fallback) }
}

@Composable
private fun LoadingImageContent(
    url: String?,
    loader: ImageLoader,
    modifier: Modifier,
    onState: ((String) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    if (url == null) {
        if (onState != null) LaunchedEffect(onState) { onState("missing_url") }
        Box(modifier.fillMaxSize()) { fallback() }
        return
    }
    val thumbnails = LocalThumbnailLoader.current
    if (thumbnails == null) {
        CoilLoadingImage(url, loader, modifier, onState, fallback)
        return
    }
    val subscription = remember(thumbnails, url) { thumbnails.subscription(url) }
    val initial = remember(thumbnails, url) { thumbnails.cached(url) }
    val delivered by subscription.bitmap.collectAsState()
    val failed by subscription.failed.collectAsState()
    val bitmap = delivered ?: initial
    if (onState != null) {
        LaunchedEffect(bitmap, failed, onState) {
            onState(
                when {
                    bitmap != null -> "thumbnail_ready"
                    failed -> "thumbnail_failed"
                    else -> "thumbnail_waiting"
                },
            )
        }
    }
    when {
        bitmap != null -> {
            Image(
                bitmap = remember(bitmap) { bitmap.asImageBitmap() },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier.fillMaxSize(),
            )
        }

        failed -> {
            CoilLoadingImage(url, loader, modifier, onState, fallback)
        }

        else -> {
            Box(modifier.fillMaxSize()) { fallback() }
        }
    }
}

@Composable
private fun CoilLoadingImage(
    url: String,
    loader: ImageLoader,
    modifier: Modifier,
    onState: ((String) -> Unit)?,
    fallback: @Composable () -> Unit,
) {
    var ready by remember(url) { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        if (!ready) fallback()
        AsyncImage(
            model = rememberMediaImageRequest(url),
            imageLoader = loader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onLoading = {
                ready = false
                onState?.invoke("coil_loading")
            },
            onSuccess = {
                ready = true
                onState?.invoke("coil_ready")
            },
            onError = {
                ready = false
                onState?.invoke("coil_failed")
            },
        )
    }
}

internal fun Modifier.mediaTransform(motion: ImageViewerMotion): Modifier =
    graphicsLayer {
        val reduction = if (motion.transform.scale == 0f) 1f else motion.imageScale / motion.transform.scale
        scaleX = motion.imageScale
        scaleY = motion.imageScale
        translationX = (motion.transform.x + motion.displacement.x) * reduction
        translationY = (motion.transform.y + motion.displacement.y) * reduction
        alpha = motion.imageAlpha
    }
