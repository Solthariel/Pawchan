package com.solthariel.pawchan.core.ui

import kotlin.math.roundToInt

/** Reachable post starts, followed by the real bottom only when it differs from a post start. */
internal data class ItemScrollAnchors(
    val end: FastScrollTarget,
) {
    val lastStep: Int = end.index + if (end.offset > 0) 1 else 0

    fun target(progress: Float): FastScrollTarget {
        if (!progress.isFinite() || lastStep <= 0) return FastScrollTarget(0, 0)
        val step = (progress.coerceIn(0f, 1f) * lastStep).roundToInt()
        return if (step >= lastStep) end else FastScrollTarget(step, 0)
    }

    fun progress(
        index: Int,
        offset: Int,
    ): Float {
        if (lastStep <= 0) return 0f
        val step = index + if (index == end.index && end.offset > 0) offset.toFloat() / end.offset else 0f
        return (step / lastStep).coerceIn(0f, 1f)
    }
}
