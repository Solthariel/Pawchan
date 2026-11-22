package com.solthariel.pawchan.media

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** Overlay is scoped to a viewer opening; paging and closing keep their existing gesture handling. */
@Composable
internal fun MediaImageOpening(
    item: MediaAttachmentUi,
    content: @Composable () -> Unit,
) {
    val store = LocalMediaThumbnailOrigin.current
    val origin = remember { store?.take(item) }
    if (origin == null) {
        content()
        return
    }
    OpeningContent(item, origin, content)
}

@Composable
private fun OpeningContent(
    item: MediaAttachmentUi,
    origin: MediaThumbnailSnapshot,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val progress = remember { Animatable(0f) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    var finished by remember { mutableStateOf(false) }
    val frame = bounds?.let { frameForImage(item, origin, it, progress.value) }
    LaunchedEffect(item.thumbnailKey()) {
        if (item.thumbnailKey() != origin.key) finished = true
    }
    LaunchedEffect(bounds) {
        val viewerBounds = bounds ?: return@LaunchedEffect
        if (finished || viewerBounds.width <= 0f || viewerBounds.height <= 0f) return@LaunchedEffect

        // onGloballyPositioned can report a zero-sized pass before the viewer is measured.
        // Don't treat that transient layout as a completed (or invalid-origin) opening.
        if (frameForImage(item, origin, viewerBounds, progress = 0f) != null) {
            progress.animateTo(1f, tween(OPENING_DURATION_MS, easing = OPENING_EASING))
        }
        finished = true
    }
    val viewerAlpha =
        if (finished) 1f else ((progress.value - VIEWER_FADE_START) / (1f - VIEWER_FADE_START)).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val p = coordinates.positionInRoot()
            val x = location[0] + p.x
            val y = location[1] + p.y
            bounds = Rect(x, y, x + coordinates.size.width, y + coordinates.size.height)
        },
    ) {
        if (!finished) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = progress.value)))
        val hidden = if (finished) Modifier else Modifier.clearAndSetSemantics { }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = viewerAlpha }.then(hidden)) { content() }
        if (!finished && frame != null) OpeningThumbnail(origin, frame, progress.value, viewerAlpha)
        if (!finished) OpeningInputBlocker()
    }
}

private fun frameForImage(
    item: MediaAttachmentUi,
    origin: MediaThumbnailSnapshot,
    bounds: Rect,
    progress: Float,
): Rect? {
    val width =
        item.dimensions
            ?.width
            ?.takeIf { it > 0 }
            ?.toFloat() ?: origin.bitmap.width.toFloat()
    val height =
        item.dimensions
            ?.height
            ?.takeIf { it > 0 }
            ?.toFloat() ?: origin.bitmap.height.toFloat()
    return openingFrame(origin.bounds, bounds, width, height, progress)
}

@Composable
private fun OpeningThumbnail(
    origin: MediaThumbnailSnapshot,
    frame: Rect,
    progress: Float,
    viewerAlpha: Float,
) {
    val density = LocalDensity.current
    val corner = with(density) { (origin.cornerPx * (1f - progress)).toDp() }
    val frameWidth = with(density) { frame.width.toDp() }
    val frameHeight = with(density) { frame.height.toDp() }
    Image(
        bitmap = remember(origin.bitmap) { origin.bitmap.asImageBitmap() },
        contentDescription = null,
        modifier =
            Modifier
                .offset { IntOffset(frame.left.roundToInt(), frame.top.roundToInt()) }
                .size(frameWidth, frameHeight)
                .graphicsLayer { alpha = 1f - viewerAlpha }
                .clip(RoundedCornerShape(corner)),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun OpeningInputBlocker() {
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        },
    )
}

private const val OPENING_DURATION_MS = 250
private const val VIEWER_FADE_START = 0.75f
private const val STANDARD_EASING_X = 0.2f
private val OPENING_EASING = CubicBezierEasing(STANDARD_EASING_X, 0f, 0f, 1f)
