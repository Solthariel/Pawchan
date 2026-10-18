package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

class FastScrollGesture {
    internal var trackHeight by mutableFloatStateOf(0f)
    var dragging by mutableStateOf(false)
        private set
    internal var progress by mutableFloatStateOf(0f)
        private set
    internal var interaction by mutableIntStateOf(0)
        private set
    private var grabOffset = 0f

    internal fun begin(
        pointerY: Float,
        currentProgress: Float,
        thumbHeight: Float,
    ) {
        val top = (trackHeight - thumbHeight).coerceAtLeast(0f) * currentProgress
        grabOffset =
            if (pointerY in top..(top + thumbHeight)) {
                pointerY - top
            } else {
                thumbHeight / 2f
            }
        interaction++
        dragging = true
        move(pointerY, thumbHeight)
    }

    internal fun move(
        pointerY: Float,
        thumbHeight: Float,
    ) {
        val travel = trackHeight - thumbHeight
        progress = if (travel > 0f) ((pointerY - grabOffset) / travel).coerceIn(0f, 1f) else 0f
    }

    internal fun finish() {
        dragging = false
    }
}

internal fun Modifier.fastScrollInput(
    available: Boolean,
    gesture: FastScrollGesture,
    progress: State<Float>,
    thumbHeight: Float,
    onProgress: (Float) -> Unit,
): Modifier =
    pointerInput(available, gesture.trackHeight, thumbHeight) {
        if (!available || gesture.trackHeight <= 0f) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown()
            gesture.begin(down.position.y, progress.value, thumbHeight)
            down.consume()
            try {
                onProgress(gesture.progress)
                drag(down.id) { change ->
                    gesture.move(change.position.y, thumbHeight)
                    change.consume()
                    onProgress(gesture.progress)
                }
            } finally {
                gesture.finish()
            }
        }
    }
