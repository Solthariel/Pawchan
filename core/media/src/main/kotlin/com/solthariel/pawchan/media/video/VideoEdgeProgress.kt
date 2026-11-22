package com.solthariel.pawchan.media.video

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Read-only timeline. No thumb, touch target or seeking semantics at the system gesture edge. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun VideoEdgeProgress(
    state: VideoPlaybackStatus,
    controlsVisible: Boolean,
    colors: ColorScheme,
    modifier: Modifier = Modifier,
) {
    if (!showVideoEdgeProgress(state, controlsVisible)) return
    val target = videoEdgeProgressFraction(state.timeline)
    val fraction =
        animateFloatAsState(
            targetValue = target,
            animationSpec = if (state.playing) tween(EDGE_PROGRESS_ANIMATION_MS, easing = LinearEasing) else snap(),
        )
    val active = videoEdgeActiveColor(colors)
    val inactive = videoEdgeInactiveColor(colors)
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(EDGE_PROGRESS_HEIGHT)
                .testTag("video-edge-progress")
                .semantics {
                    contentDescription = "Прогресс видео"
                    progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                },
        ) {
            drawRect(inactive)
            val width = size.width * fraction.value
            val start = videoSeekTrackBoundary(size.width, 0f, layoutDirection == LayoutDirection.Rtl)
            val left = if (layoutDirection == LayoutDirection.Rtl) start - width else start
            drawRect(
                active,
                topLeft = Offset(left, 0f),
                size = Size(width, size.height),
            )
        }
    }
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
private const val EDGE_PROGRESS_ANIMATION_MS = 200
