package com.solthariel.pawchan.media.video

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
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
        key(player) { VideoRenderSurface(player, video, motion) }
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
internal fun VideoRenderSurface(
    player: VideoPlaybackSession,
    video: MediaAttachmentUi,
    motion: ImageViewerMotion,
) {
    val output by player.hasVideoOutput.collectAsState()
    val playbackStatus = player.status.collectAsState()
    val actualAspect by remember(playbackStatus) { derivedStateOf { playbackStatus.value.videoAspect } }
    val aspect = videoContentAspect(actualAspect, video.dimensions?.width, video.dimensions?.height)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                FrameLayout(context).apply {
                    contentDescription = video.fileName
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
                view.contentDescription = video.fileName
                if (!player.inPip) player.engine.attach(view)
            },
            modifier =
                Modifier.videoContentLayout(aspect).graphicsLayer {
                    val reduction = motion.imageScale / motion.transform.scale
                    scaleX = motion.imageScale
                    scaleY = motion.imageScale
                    translationX = (motion.transform.x + motion.displacement.x) * reduction
                    translationY = (motion.transform.y + motion.displacement.y) * reduction
                    alpha = motion.imageAlpha * videoSurfaceAlpha(output)
                },
        )
    }
}

private fun Modifier.videoContentLayout(aspect: Double?): Modifier =
    layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
            val child = measurable.measure(constraints)
            layout(child.width, child.height) { child.placeRelative(0, 0) }
        } else {
            val size = videoContentSize(constraints.maxWidth, constraints.maxHeight, aspect)
            val child = measurable.measure(Constraints.fixed(size.width, size.height))
            layout(child.width, child.height) { child.placeRelative(0, 0) }
        }
    }
