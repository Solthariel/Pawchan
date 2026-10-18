package com.solthariel.pawchan.thread

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed

/** Leave MOVE unconsumed so the enclosing list can take scrolling ownership in the final pass. */
internal suspend fun PointerInputScope.detectPostTextGestures(actions: () -> PostTextGestures) {
    val config = viewConfiguration
    val timing =
        PostTextGestureTiming(
            config.doubleTapMinTimeMillis,
            config.doubleTapTimeoutMillis,
            config.touchSlop * DOUBLE_TAP_SLOP_FACTOR,
        )
    val session = PostTextGestureSession(actions, config.touchSlop, timing)
    try {
        awaitEachGesture {
            val down = awaitFirstDown()
            session.down(down.position, down.uptimeMillis)
            down.consume()
            try {
                val released =
                    if (session.acceptsLongPress) {
                        withTimeoutOrNull(config.longPressTimeoutMillis) { awaitPostTextRelease(session, down.id) }
                    } else {
                        awaitPostTextRelease(session, down.id)
                    }
                if (released == null && session.isActive) {
                    session.longPress()
                    consumePostTextRemainder()
                }
            } finally {
                session.cancel()
            }
        }
    } finally {
        session.cancel()
    }
}

private suspend fun AwaitPointerEventScope.awaitPostTextRelease(
    session: PostTextGestureSession,
    pointer: PointerId,
): Boolean {
    while (session.isActive) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointer }
        val extraPointer = event.changes.any { it.id != pointer && it.pressed }
        if (change == null || change.isConsumed || extraPointer) {
            session.cancel()
        } else {
            session.move(change.position)
            if (session.isActive && change.changedToUpIgnoreConsumed()) {
                change.consume()
                session.up(change.uptimeMillis)
                return true
            }
            if (session.isActive && awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) {
                session.cancel()
            }
        }
    }
    return false
}

private suspend fun AwaitPointerEventScope.consumePostTextRemainder() {
    do {
        val event = awaitPointerEvent()
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

private const val DOUBLE_TAP_SLOP_FACTOR = 2f
