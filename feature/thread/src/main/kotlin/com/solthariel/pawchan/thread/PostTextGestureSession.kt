package com.solthariel.pawchan.thread

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

internal data class PostTextTarget(
    val tag: String,
    val item: String,
    val start: Int,
    val end: Int,
)

internal data class PostTextGestureTiming(
    val minimumDoubleTapMillis: Long,
    val maximumDoubleTapMillis: Long,
    val doubleTapSlop: Float,
)

private data class PostTextPress(
    val position: Offset,
    val downTimeMillis: Long,
    val target: PostTextTarget?,
    val actions: PostTextGestures,
)

private data class PlainPostTap(
    val position: Offset,
    val upTimeMillis: Long,
)

/** Owns one press. Interactive text never enters the plain-text double-tap sequence. */
internal class PostTextGestureSession(
    private val actions: () -> PostTextGestures,
    private val touchSlop: Float,
    private val timing: PostTextGestureTiming,
) {
    private var press: PostTextPress? = null
    private var lastPlainTap: PlainPostTap? = null
    val isActive: Boolean get() = press != null
    val acceptsLongPress: Boolean get() = press?.target?.tag != SPOILER_TAG

    fun down(
        position: Offset,
        timeMillis: Long,
    ) {
        cancel()
        val owner = actions()
        val target = owner.targetAt(position)
        if (target != null) lastPlainTap = null
        press = PostTextPress(position, timeMillis, target, owner)
        owner.onPress(position)
    }

    fun move(position: Offset) {
        val held = press ?: return
        val delta = position - held.position
        val outsideSlop = abs(delta.x) > touchSlop || abs(delta.y) > touchSlop
        if (outsideSlop && (held.target == null || held.actions.targetAt(position) != held.target)) cancel()
    }

    fun up(timeMillis: Long) {
        val held = press ?: return
        press = null
        try {
            if (held.target != null) {
                held.actions.onTap(held.position)
            } else {
                plainTap(held, timeMillis)
            }
        } finally {
            held.actions.onRelease()
        }
    }

    fun longPress() {
        val held = press ?: return
        if (!acceptsLongPress) return
        press = null
        lastPlainTap = null
        held.actions.onRelease()
        held.actions.onLongPress(held.position)
    }

    fun cancel() {
        val held = press ?: return
        press = null
        lastPlainTap = null
        held.actions.onRelease()
    }

    private fun plainTap(
        held: PostTextPress,
        timeMillis: Long,
    ) {
        val previous = lastPlainTap
        val interval = previous?.let { held.downTimeMillis - it.upTimeMillis }
        val doubleTap =
            previous != null && interval != null &&
                interval in timing.minimumDoubleTapMillis..timing.maximumDoubleTapMillis &&
                (held.position - previous.position).getDistance() <= timing.doubleTapSlop
        if (doubleTap) {
            lastPlainTap = null
            held.actions.onDoubleTap(held.position)
        } else {
            lastPlainTap = PlainPostTap(held.position, timeMillis)
            held.actions.onTap(held.position)
        }
    }
}
