package com.solthariel.pawchan.boards

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
internal fun Modifier.forumSwipeGesture(onSwipe: ((BoardSwipeDirection) -> Unit)?): Modifier {
    val currentAction = rememberUpdatedState(onSwipe)
    val threshold = with(LocalDensity.current) { BOARD_SWIPE_THRESHOLD.toPx() }
    return pointerInput(onSwipe != null, threshold) {
        if (onSwipe == null) return@pointerInput
        var horizontalDistance = 0f
        var handled = false
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount ->
                if (!handled) {
                    horizontalDistance += dragAmount
                    if (abs(horizontalDistance) >= threshold) {
                        handled = true
                        currentAction.value?.invoke(
                            if (horizontalDistance < 0f) {
                                BoardSwipeDirection.NEXT
                            } else {
                                BoardSwipeDirection.PREVIOUS
                            },
                        )
                    }
                }
            },
            onDragEnd = {
                horizontalDistance = 0f
                handled = false
            },
            onDragCancel = {
                horizontalDistance = 0f
                handled = false
            },
        )
    }
}

private val BOARD_SWIPE_THRESHOLD = 48.dp
