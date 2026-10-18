package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import coil3.ImageLoader
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaLoadingImage
import com.solthariel.pawchan.media.mediaTransform
import com.solthariel.pawchan.media.resolvedMediaUrl

/** Session-owned poster with an immediate opaque reset and a reserved image on thumbnail failure. */
@Composable
internal fun VideoPosterLayer(
    player: VideoPlaybackSession,
    video: MediaAttachmentUi,
    imageLoader: ImageLoader,
    motion: ImageViewerMotion,
    hasVideoOutput: Boolean,
) {
    val reserve = rememberVideoPosterReserve(player)
    val posterAlpha = rememberVideoPosterAlpha(hasVideoOutput, player)
    val posterShown by remember(posterAlpha) { derivedStateOf { posterAlpha.value > 0f } }
    BindVideoPosterDiagnostics(player, hasVideoOutput, posterAlpha, motion)
    val posterTrace = rememberPosterTrace(player)
    if (!hasVideoOutput || posterShown) {
        val poster = video.thumbnailUrl?.let { resolvedMediaUrl(it) }
        MediaLoadingImage(
            poster,
            imageLoader,
            Modifier.mediaTransform(motion).graphicsLayer { alpha = posterAlpha.value }.drawWithContent {
                drawContent()
                posterTrace.draw(posterAlpha.value)
            },
            onState = remember(posterTrace) { posterTrace::record },
            fallback = { VideoPosterFallback(reserve) },
        )
    }
}
