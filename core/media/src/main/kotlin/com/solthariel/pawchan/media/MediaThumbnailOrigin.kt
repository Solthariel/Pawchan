package com.solthariel.pawchan.media

import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading

/** Captured only on an actual thumbnail click; missing or offscreen images skip the transition. */
internal data class MediaThumbnailSnapshot(
    val key: MediaThumbnailKey,
    val bounds: Rect,
    val cornerPx: Float,
    val bitmap: Bitmap,
)

internal data class MediaThumbnailKey(
    val url: String,
    val id: String,
    val post: Long?,
)

internal fun MediaAttachmentUi.thumbnailKey() = MediaThumbnailKey(originalUrl, id, source.ownerPostId)

internal class MediaThumbnailOriginStore {
    private var pending: MediaThumbnailSnapshot? = null

    fun put(value: MediaThumbnailSnapshot?) {
        pending = value
    }

    fun take(item: MediaAttachmentUi): MediaThumbnailSnapshot? {
        val value = pending
        pending = null
        return value?.takeIf { it.key == item.thumbnailKey() }
    }
}

internal val LocalMediaThumbnailOrigin = staticCompositionLocalOf<MediaThumbnailOriginStore?> { null }

/** Kept by the thumbnail, not by the lazy list or an application-wide registry. */
class MediaThumbnailOrigin internal constructor(
    private val store: MediaThumbnailOriginStore?,
    private val view: View,
    private val loader: ThumbnailLoader?,
    private val item: MediaAttachmentUi,
    private val cornerPx: Float,
) {
    private var coordinates: LayoutCoordinates? = null

    fun place(value: LayoutCoordinates) {
        coordinates = value
    }

    fun capture(enabled: Boolean = true) {
        val position = coordinates?.takeIf { it.isAttached }
        val url = item.thumbnailUrl
        val bitmap =
            if (enabled && item.viewer == AttachmentViewer.IMAGE && url != null) {
                loader?.cached(url)
            } else {
                null
            }
        val snapshot =
            if (position != null && bitmap != null) {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                val point = position.positionInRoot()
                val x = location[0] + point.x
                val y = location[1] + point.y
                val size = position.size
                val bounds = Rect(x, y, x + size.width, y + size.height)
                MediaThumbnailSnapshot(item.thumbnailKey(), bounds, cornerPx, bitmap)
            } else {
                null
            }
        store?.put(snapshot)
    }
}

@Composable
fun rememberMediaThumbnailOrigin(
    item: MediaAttachmentUi,
    loading: ThumbnailLoading,
    corner: Dp,
): MediaThumbnailOrigin = rememberMediaThumbnailOrigin(item, loading.thumbnailLoader, corner)

@Composable
fun rememberMediaThumbnailOrigin(
    item: MediaAttachmentUi,
    loader: ThumbnailLoader?,
    corner: Dp,
): MediaThumbnailOrigin {
    val store = LocalMediaThumbnailOrigin.current
    val view = LocalView.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { corner.toPx() }
    return remember(store, view, loader, item, px) { MediaThumbnailOrigin(store, view, loader, item, px) }
}
