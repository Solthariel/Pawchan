package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.ConstraintsSizeResolver
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver

/** Only the selected page and its immediate neighbors own painters; leaving the window releases them. */
internal data class ViewerImagePages(
    val painters: Map<String, AsyncImagePainter>,
    val sizeResolver: ConstraintsSizeResolver,
    val originals: Boolean,
) {
    operator fun get(url: String): AsyncImagePainter = checkNotNull(painters[url])
}

@Composable
internal fun rememberViewerImagePages(
    image: MediaAttachmentUi,
    navigation: MediaViewerNavigation,
    loader: ImageLoader,
    retryToken: Int,
): ViewerImagePages {
    val originals = neighborOriginalsAllowed()
    val neighbors = mutableListOf<String>()
    for (item in listOfNotNull(navigation.previousItem, navigation.nextItem)) {
        if (item.viewer != AttachmentViewer.IMAGE) continue
        val url = if (originals) item.originalUrl else item.thumbnailUrl
        if (url != null) neighbors += resolvedMediaUrl(url)
    }
    val thumbnail = if (originals) null else image.thumbnailUrl?.let { resolvedMediaUrl(it) }
    val keys = viewerImagePageKeys(resolvedMediaUrl(image.originalUrl), thumbnail, neighbors, retryToken)
    val sizeResolver = rememberConstraintsSizeResolver()
    return ViewerImagePages(rememberViewerImagePainters(keys, loader, sizeResolver), sizeResolver, originals)
}

/** Keyed by resource, not by active/previous/next role: a running GIF survives the swipe handoff. */
@Composable
internal fun rememberViewerImagePainters(
    keys: List<ViewerImagePageKey>,
    loader: ImageLoader,
    sizeResolver: ConstraintsSizeResolver,
): Map<String, AsyncImagePainter> {
    val painters = LinkedHashMap<String, AsyncImagePainter>()
    for (page in keys) {
        painters[page.url] =
            key(page, loader, sizeResolver) {
                val base = rememberMediaImageRequest(page.url, page.retryToken)
                val request = remember(base, sizeResolver) { base.newBuilder().size(sizeResolver).build() }
                rememberAsyncImagePainter(request, loader, contentScale = ContentScale.Fit)
            }
    }
    return painters
}
