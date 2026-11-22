package com.solthariel.pawchan.composer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** Tools cancel their one-finger gesture when a second finger starts viewport navigation. */
internal fun Modifier.imageViewportInput(
    enabled: Boolean,
    mode: ImageEditorCanvasMode,
    onTransform: (ImageViewportMotion) -> Unit,
): Modifier =
    pointerInput(enabled, mode) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                if (event.changes.count { it.pressed && it.previousPressed } >= 2) {
                    val center = event.calculateCentroid(useCurrent = false)
                    val pan = event.calculatePan()
                    onTransform(
                        ImageViewportMotion(
                            event.calculateZoom(),
                            pan.x,
                            pan.y,
                            center.x - size.width / 2f,
                            center.y - size.height / 2f,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        ),
                    )
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
