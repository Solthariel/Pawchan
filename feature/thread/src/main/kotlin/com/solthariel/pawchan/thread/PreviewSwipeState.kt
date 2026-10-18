package com.solthariel.pawchan.thread

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.sign

internal enum class PreviewSwipeAction { RESTORE, POP, CLEAR }

/** Distances are dp, so phone density does not change the meaning of a gesture. */
internal fun previewSwipeAction(
    distance: Float,
    height: Float,
    velocity: Float,
): PreviewSwipeAction {
    val threshold = (height * DISMISS_FRACTION).coerceIn(MIN_DISMISS_DISTANCE, MAX_DISMISS_DISTANCE)
    val fling = abs(distance) >= MIN_FLING_DISTANCE && abs(velocity) >= DISMISS_VELOCITY && distance * velocity > 0
    return when {
        abs(distance) < threshold && !fling -> PreviewSwipeAction.RESTORE
        distance < 0 -> PreviewSwipeAction.POP
        else -> PreviewSwipeAction.CLEAR
    }
}

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
        offset += delta
        return delta
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
                previewSwipeAction(offset / density, height / density, velocity / density)
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
            Animatable(offset).animateTo(destination, tween(SWIPE_DURATION)) { offset = value }
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
}

private const val DISMISS_FRACTION = 0.28f
private const val MIN_DISMISS_DISTANCE = 48f
private const val MAX_DISMISS_DISTANCE = 140f
private const val MIN_FLING_DISTANCE = 24f
private const val DISMISS_VELOCITY = 1200f
private const val EXIT_DISTANCE = 1.5f
private const val SWIPE_DURATION = 160
