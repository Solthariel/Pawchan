package com.solthariel.pawchan.media.video

import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Uses the renderer's display aspect (including rotation), falling back to attachment metadata. */
internal fun videoContentAspect(
    actual: Double?,
    width: Int?,
    height: Int?,
): Double? {
    val decoded = actual?.takeIf { it.isFinite() && it > 0.0 }
    val validWidth = width?.takeIf { it > 0 }
    val validHeight = height?.takeIf { it > 0 }
    val dimensions = if (validWidth != null && validHeight != null) validWidth.toDouble() / validHeight else null
    return decoded ?: dimensions
}

/** Пропорции кадра на экране: от плеера, а до первого кадра — из размеров вложения. */
internal fun videoFrameAspect(
    state: VideoPlaybackStatus,
    video: com.solthariel.pawchan.media.MediaAttachmentUi,
): Double? = videoContentAspect(state.videoAspect, video.dimensions?.width, video.dimensions?.height)

/** Fits without cropping; unknown aspect keeps the surface visible rather than hiding the video. */
internal fun videoContentSize(
    width: Int,
    height: Int,
    aspect: Double?,
): IntSize =
    when {
        width <= 0 || height <= 0 -> IntSize(width.coerceAtLeast(0), height.coerceAtLeast(0))
        aspect == null || !aspect.isFinite() || aspect <= 0.0 -> IntSize(width, height)
        aspect >= width.toDouble() / height -> IntSize(width, (width / aspect).roundToInt().coerceIn(1, height))
        else -> IntSize((height * aspect).roundToInt().coerceIn(1, width), height)
    }
