package com.solthariel.pawchan.media.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R.string as L

/**
 * Read-only timeline on the bottom edge of the video frame (its width), never under the system navigation.
 * No thumb, touch target or seeking semantics.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun VideoEdgeProgress(
    state: VideoPlaybackStatus,
    controlsVisible: Boolean,
    colors: ColorScheme,
    aspect: Double?,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
) {
    val res = LocalResources.current
    if (!showVideoEdgeProgress(state, controlsVisible)) return
    val position = rememberSmoothVideoPosition(state, speed)
    val target = videoEdgeProgressFraction(state.timeline)
    val active = videoEdgeActiveColor(colors)
    val inactive = videoEdgeInactiveColor(colors)
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val bars = WindowInsets.navigationBarsIgnoringVisibility
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val safeRight = width - bars.getRight(density, direction)
        val safe = IntRect(bars.getLeft(density, direction), 0, safeRight, height - bars.getBottom(density))
        val thickness = with(density) { EDGE_PROGRESS_HEIGHT.roundToPx() }
        val rect = videoEdgeProgressRect(IntSize(width, height), aspect, safe, thickness)
        Canvas(
            Modifier
                .absoluteOffset { IntOffset(rect.left, rect.top) }
                .size(with(density) { rect.width.toDp() }, EDGE_PROGRESS_HEIGHT)
                .testTag("video-edge-progress")
                .semantics {
                    contentDescription = res.getString(L.ui_video_progress)
                    progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                },
        ) {
            drawRect(inactive)
            val filled = size.width * videoEdgeProgressFraction(VideoTimeline(position.value, state.duration))
            val begin = videoSeekTrackBoundary(size.width, 0f, layoutDirection == LayoutDirection.Rtl)
            val left = if (layoutDirection == LayoutDirection.Rtl) begin - filled else begin
            drawRect(active, topLeft = Offset(left, 0f), size = Size(filled, size.height))
        }
    }
}

/** Нижний край кадра по его ширине; кадр вписан по центру, как в [videoContentSize]. */
internal fun videoEdgeProgressRect(
    container: IntSize,
    aspect: Double?,
    safe: IntRect,
    thickness: Int,
): IntRect {
    val content = videoContentSize(container.width, container.height, aspect)
    val frameLeft = (container.width - content.width) / 2
    val left = maxOf(frameLeft, safe.left)
    val right = minOf(frameLeft + content.width, safe.right)
    val bottom = minOf((container.height + content.height) / 2, safe.bottom)
    return IntRect(left, bottom - thickness, maxOf(left, right), bottom)
}

internal fun showVideoEdgeProgress(
    state: VideoPlaybackStatus,
    controlsVisible: Boolean,
): Boolean = !controlsVisible && state.duration > 0L && state.stage != VideoStage.ERROR

internal fun videoEdgeProgressFraction(timeline: VideoTimeline): Float =
    if (timeline.duration > 0L) {
        (timeline.position.toDouble() / timeline.duration).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

internal fun videoEdgeActiveColor(colors: ColorScheme) = videoSeekActiveColor(colors, true).copy(alpha = 0.65f)

internal fun videoEdgeInactiveColor(colors: ColorScheme) = videoSeekInactiveColor(colors, true).copy(alpha = 0.25f)

private val EDGE_PROGRESS_HEIGHT = 2.dp
