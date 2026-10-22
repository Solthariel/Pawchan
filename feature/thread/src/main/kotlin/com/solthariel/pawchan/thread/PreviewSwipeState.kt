package com.solthariel.pawchan.thread

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

internal enum class PreviewSwipeAction { RESTORE, POP, CLEAR }

internal class PreviewSwipeState {
    var offset by mutableFloatStateOf(0f)
        private set
    var height = 1f
    var viewportHeight = 0f
    private var settling = false
    private var released = false

    fun beginGesture() {
        released = false
    }

    fun endGesture() {
        released = true
    }

    fun cancelGesture() {
        offset = 0f
        released = false
    }

    val progress: Float get() = (abs(offset) / height.coerceAtLeast(1f)).coerceIn(0f, 1f)

    fun drag(delta: Float): Float {
        if (settling) return 0f
        val crossingZero = offset * delta < 0f && offset * (offset + delta) <= 0f
        val consumed = if (crossingZero) -offset else delta
        offset = if (crossingZero) 0f else offset + consumed
        return consumed
    }

    suspend fun release(
        velocity: Float,
        density: Float,
        pop: () -> Unit,
        clear: () -> Unit,
    ) {
        if (offset == 0f || settling) return
        settling = true
        val action =
            if (released) {
                previewSwipeAction(
                    offset / density,
                    maxOf(height, viewportHeight) / density,
                    velocity / density,
                    height / density,
                )
            } else {
                PreviewSwipeAction.RESTORE
            }
        val destination =
            if (action == PreviewSwipeAction.RESTORE) {
                0f
            } else {
                sign(offset) * maxOf(height, viewportHeight) * EXIT_DISTANCE
            }
        var dismissed = false
        try {
            if (action == PreviewSwipeAction.RESTORE) {
                settle(destination, velocity)
            } else {
                withTimeoutOrNull(PREVIEW_CLOSE_DELAY) { settle(destination, velocity) }
            }
            when (action) {
                PreviewSwipeAction.POP -> {
                    pop()
                    dismissed = true
                }

                PreviewSwipeAction.CLEAR -> {
                    clear()
                    dismissed = true
                }

                PreviewSwipeAction.RESTORE -> {
                    Unit
                }
            }
        } finally {
            if (!dismissed) offset = 0f
            settling = false
        }
    }

    private suspend fun settle(
        destination: Float,
        velocity: Float,
    ) {
        Animatable(offset).animateTo(
            destination,
            tween(
                previewSwipeDuration(destination - offset, velocity, maxOf(height, viewportHeight)),
                easing = PREVIEW_RESTORE_EASING,
            ),
        ) { offset = value }
    }
}

private const val EXIT_DISTANCE = 1.5f

private const val PREVIEW_CLOSE_DELAY = 150L
