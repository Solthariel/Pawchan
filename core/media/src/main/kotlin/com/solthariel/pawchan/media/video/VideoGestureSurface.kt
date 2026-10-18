package com.solthariel.pawchan.media.video

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.ImageViewport
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.mediaViewerGestures

@Composable
internal fun VideoGestureSurface(
    player: VideoPlaybackSession,
    video: MediaAttachmentUi,
    motion: ImageViewerMotion,
    navigation: MediaViewerNavigation,
    onClose: () -> Unit,
    onLongPress: () -> Unit,
) {
    Box(Modifier.fillMaxSize().clipToBounds()) {
        key(player) { VideoRenderSurface(player, video.fileName, motion) }
        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    motion.resize(
                        ImageViewport(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            video.dimensions?.width?.toFloat() ?: size.width.toFloat(),
                            video.dimensions?.height?.toFloat() ?: size.height.toFloat(),
                        ),
                    )
                }.mediaViewerGestures(motion, navigation, onClose, onLongPress),
        )
    }
}

@Composable
private fun VideoRenderSurface(
    player: VideoPlaybackSession,
    fileName: String,
    motion: ImageViewerMotion,
) {
    val output by player.hasVideoOutput.collectAsState()
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                contentDescription = fileName
                keepScreenOn = true
                VideoHandoffDiagnostics.record(
                    player,
                    "host_create host=${System.identityHashCode(this)} output=$output",
                )
                if (!player.inPip) player.engine.attach(this)
            }
        },
        onReset = null,
        onRelease = {
            VideoHandoffDiagnostics.record(player, "host_release host=${System.identityHashCode(it)}")
            player.engine.detach(it)
        },
        update = { view ->
            VideoHandoffDiagnostics.record(
                player,
                "host_update host=${System.identityHashCode(view)} output=$output " +
                    "surface_alpha=${videoSurfaceAlpha(output)}",
            )
            view.contentDescription = fileName
            if (!player.inPip) player.engine.attach(view)
        },
        modifier =
            Modifier.fillMaxSize().graphicsLayer {
                val reduction = motion.imageScale / motion.transform.scale
                scaleX = motion.imageScale
                scaleY = motion.imageScale
                translationX = (motion.transform.x + motion.displacement.x) * reduction
                translationY = (motion.transform.y + motion.displacement.y) * reduction
                alpha = motion.imageAlpha * videoSurfaceAlpha(output)
            },
    )
}
