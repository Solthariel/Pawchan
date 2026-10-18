package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.R

/** Edge overlay: no separate column, persistent background, or content-width animation. */
@Composable
fun PawchanFastScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    itemSizes: FastScrollItemSizes? = null,
    inputGesture: FastScrollGesture? = null,
    markers: FastScrollMarkers = FastScrollMarkers(),
    mode: FastScrollMode = FastScrollMode.CONTENT,
) {
    FastScrollbarCanvas(state, modifier, itemSizes, inputGesture, markers, FastScrollbarSettings(mode))
}

/** A 2dp overlay for row-based grids, without any content inset or width reservation. */
@Composable
fun PawchanThinRowScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    inputGesture: FastScrollGesture? = null,
) {
    FastScrollbarCanvas(
        state,
        modifier,
        null,
        inputGesture,
        FastScrollMarkers(),
        FastScrollbarSettings(FastScrollMode.ITEMS, 2.dp),
    )
}

private data class FastScrollbarSettings(
    val mode: FastScrollMode,
    val width: androidx.compose.ui.unit.Dp = FAST_SCROLL_BAR_WIDTH,
)

@Composable
private fun FastScrollbarCanvas(
    state: LazyListState,
    modifier: Modifier,
    itemSizes: FastScrollItemSizes?,
    inputGesture: FastScrollGesture?,
    markers: FastScrollMarkers,
    settings: FastScrollbarSettings,
) {
    val mode = settings.mode
    val scope = rememberCoroutineScope()
    val controller = remember(state, scope) { FastScrollController(state, scope) }
    val gesture = inputGesture ?: remember(state) { FastScrollGesture() }
    val suppliedSizes = rememberUpdatedState(itemSizes)
    val geometry = remember(state) { derivedStateOf { state.fastScrollGeometry(suppliedSizes.value) } }
    val progress = rememberFastScrollProgress(state, mode, geometry)
    val available by remember(state) { derivedStateOf { state.canScrollBackward || state.canScrollForward } }
    val reveal =
        rememberFastScrollbarReveal(
            available,
            state.isScrollInProgress || gesture.dragging,
            gesture.interaction,
        )
    val style = fastScrollbarStyle(settings.width)
    val markerStyle = fastScrollbarMarkerStyle()
    val description = stringResource(R.string.core_ui_fast_scrollbar)
    val onProgress: (Float) -> Unit = { controller.request(it, geometry.value, mode) }
    if (!available) return
    Canvas(
        modifier =
            modifier
                .fillMaxHeight()
                .width(FAST_SCROLL_TOUCH_WIDTH)
                .clipToBounds()
                .onSizeChanged { gesture.trackHeight = it.height.toFloat() }
                .fastScrollInput(available, gesture, progress, style.thumbHeight, onProgress)
                .semantics {
                    if (available && reveal.value > 0f) {
                        contentDescription = description
                        progressBarRangeInfo = ProgressBarRangeInfo(progress.value, 0f..1f)
                        setProgress { value ->
                            onProgress(value)
                            true
                        }
                    }
                },
    ) {
        if (available && markers.itemCount > 1) {
            drawFastScrollbarMarkers(markers, markerStyle)
        }
        if (available && reveal.value > 0f) {
            val position = if (gesture.dragging) gesture.progress else progress.value
            drawFastScrollbar(style, position, reveal.value, gesture.dragging)
        }
    }
}

@Composable
private fun rememberFastScrollProgress(
    state: LazyListState,
    mode: FastScrollMode,
    geometry: State<FastScrollGeometry>,
): State<Float> =
    remember(state, mode, geometry) {
        derivedStateOf {
            if (mode == FastScrollMode.ITEMS) {
                itemScrollProgress(
                    state.firstVisibleItemIndex,
                    geometry.value.totalItems,
                    state.fullyVisibleItems(),
                    state.canScrollBackward,
                    state.canScrollForward,
                )
            } else {
                geometry.value.progress(
                    state.firstVisibleItemIndex,
                    state.firstVisibleItemScrollOffset,
                    state.canScrollBackward,
                    state.canScrollForward,
                )
            }
        }
    }

data class FastScrollMarkers(
    val itemCount: Int = 0,
    val own: List<Int> = emptyList(),
    val replies: List<Int> = emptyList(),
)

@Composable
private fun fastScrollbarMarkerStyle(): FastScrollbarMarkerStyle {
    val colors = MaterialTheme.colorScheme
    return with(LocalDensity.current) {
        FastScrollbarMarkerStyle(
            width = FAST_SCROLL_MARKER_WIDTH.toPx(),
            height = FAST_SCROLL_MARKER_HEIGHT.toPx(),
            own = colors.primary,
            reply = colors.tertiary,
        )
    }
}

private fun DrawScope.drawFastScrollbarMarkers(
    markers: FastScrollMarkers,
    style: FastScrollbarMarkerStyle,
) {
    fun marker(
        index: Int,
        color: Color,
    ) {
        val progress = index.toFloat() / (markers.itemCount - 1).coerceAtLeast(1)
        drawRect(
            color = color,
            topLeft = Offset(size.width - style.width, (size.height - style.height) * progress),
            size = Size(style.width, style.height),
        )
    }
    markers.own.forEach { marker(it, style.own) }
    markers.replies.filterNot(markers.own::contains).forEach { marker(it, style.reply) }
}

@Composable
private fun fastScrollbarStyle(width: androidx.compose.ui.unit.Dp): FastScrollbarStyle {
    val colors = MaterialTheme.colorScheme
    return with(LocalDensity.current) {
        FastScrollbarStyle(
            width = width.toPx(),
            thumbHeight = FAST_SCROLL_THUMB_HEIGHT.toPx(),
            normal = colors.onSurface.copy(alpha = NORMAL_THUMB_ALPHA),
            pressed = colors.primary,
            track = colors.onSurface.copy(alpha = TRACK_ALPHA),
        )
    }
}

private fun DrawScope.drawFastScrollbar(
    style: FastScrollbarStyle,
    progress: Float,
    reveal: Float,
    pressed: Boolean,
) {
    val width = style.width.coerceAtMost(size.width)
    val height = style.thumbHeight.coerceAtMost(size.height)
    val left = size.width - width * reveal.coerceIn(0f, 1f)
    drawRect(
        color = style.track,
        topLeft = Offset(left, 0f),
        size = Size(width, size.height),
    )
    drawRect(
        color = if (pressed) style.pressed else style.normal,
        topLeft = Offset(left, (size.height - height) * progress),
        size = Size(width, height),
    )
}

private data class FastScrollbarStyle(
    val width: Float,
    val thumbHeight: Float,
    val normal: Color,
    val pressed: Color,
    val track: Color,
)

private data class FastScrollbarMarkerStyle(
    val width: Float,
    val height: Float,
    val own: Color,
    val reply: Color,
)

// Reserve only the drawn width + 2 dp in card padding; the 16 dp touch target overlays it.
val FAST_SCROLL_CONTENT_INSET = 10.dp
val FAST_SCROLL_BAR_WIDTH = 8.dp
private val FAST_SCROLL_TOUCH_WIDTH = 16.dp
private val FAST_SCROLL_THUMB_HEIGHT = 48.dp
private val FAST_SCROLL_MARKER_WIDTH = 8.dp
private val FAST_SCROLL_MARKER_HEIGHT = 3.dp
private const val NORMAL_THUMB_ALPHA = 0.35f
private const val TRACK_ALPHA = 0.12f
