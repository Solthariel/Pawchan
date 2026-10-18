package com.solthariel.pawchan.thread

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Observe short taps without consuming the DOWN or long press used by SelectionContainer. */
internal fun Modifier.richTextTaps(
    key: Any,
    onTap: (Offset) -> Unit,
): Modifier =
    composed {
        val currentTap by rememberUpdatedState(onTap)
        pointerInput(key) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null && up.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                    currentTap(up.position)
                }
            }
        }
    }
