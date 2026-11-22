package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.solthariel.pawchan.domain.drafts.DraftTextGesture
import com.solthariel.pawchan.domain.drafts.ImageCanvasPoint

/** One pointer session, including switching finger count, creates one history entry. */
@Composable
internal fun ImageTextTransformOverlay(
    state: ImageEditorState,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier.semantics { contentDescription = "Переместить текст" }.pointerInput(state, index) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (size.width <= 0 || size.height <= 0) return@awaitEachGesture
                val gesture = DraftTextGesture(state.edit, index)
                var pan = Offset.Zero
                var scale = 1f
                var angle = 0f
                var complete = false
                state.begin()
                try {
                    do {
                        val event = awaitPointerEvent()
                        pan += event.calculatePan()
                        scale = (scale * event.calculateZoom()).coerceIn(MIN_GESTURE_SCALE, MAX_GESTURE_SCALE)
                        angle += event.calculateRotation()
                        val delta = ImageCanvasPoint(pan.x / size.width, pan.y / size.height)
                        state.preview(gesture.update(delta, scale, angle))
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    complete = true
                } finally {
                    if (complete) state.finish() else state.cancel()
                }
            }
        },
    ) {}
}

private const val MIN_GESTURE_SCALE = .001f
private const val MAX_GESTURE_SCALE = 1000f
