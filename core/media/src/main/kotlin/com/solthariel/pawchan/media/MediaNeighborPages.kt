package com.solthariel.pawchan.media

import androidx.compose.foundation.Image
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
    pages: ViewerImagePages? = null,
) {
    val originals = pages?.originals ?: neighborOriginalsAllowed()
    navigation.previousItem?.let { MediaNeighborPage(it, loader, motion, -1f, originals, pages) }
    navigation.nextItem?.let { MediaNeighborPage(it, loader, motion, 1f, originals, pages) }
}

@Composable
private fun MediaNeighborPage(
    item: MediaAttachmentUi,
    loader: ImageLoader,
    motion: ImageViewerMotion,
    side: Float,
    originals: Boolean,
    pages: ViewerImagePages?,
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
    val resolved = url?.let { resolvedMediaUrl(it) }
    val painter = resolved?.let { pages?.painters?.get(it) }
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().then(shift),
        )
        return
    }
    AsyncImage(
        model =
            rememberMediaImageRequest(
                resolved,
            ),
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().then(shift),
    )
}
