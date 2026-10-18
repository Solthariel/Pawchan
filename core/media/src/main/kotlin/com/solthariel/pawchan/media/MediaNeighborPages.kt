package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.solthariel.pawchan.media.video.VideoPosterFallback

/** The adjacent page is visible during dragging and settling, not replaced by a black jump. */
@Composable
internal fun MediaNeighborPages(
    navigation: MediaViewerNavigation,
    loader: ImageLoader,
    motion: ImageViewerMotion,
) {
    val originals = neighborOriginalsAllowed()
    navigation.previousItem?.let { MediaNeighborPage(it, loader, motion, -1f, originals) }
    navigation.nextItem?.let { MediaNeighborPage(it, loader, motion, 1f, originals) }
}

@Composable
private fun MediaNeighborPage(
    item: MediaAttachmentUi,
    loader: ImageLoader,
    motion: ImageViewerMotion,
    side: Float,
    originals: Boolean,
) {
    val shift =
        Modifier.graphicsLayer {
            translationX = motion.displacement.x + side * motion.pageExtent
        }
    if (item.viewer == AttachmentViewer.VIDEO) {
        // Та же картинка и тем же способом, что и превью на месте видео: после свайпа ничего не подменяется.
        MediaLoadingImage(
            item.thumbnailUrl?.let { resolvedMediaUrl(it) },
            loader,
            shift,
            fallback = { VideoPosterFallback() },
        )
        return
    }
    val url = if (originals) item.originalUrl else item.thumbnailUrl
    AsyncImage(
        model =
            rememberMediaImageRequest(
                url?.let { resolvedMediaUrl(it) },
            ),
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().then(shift),
    )
}
