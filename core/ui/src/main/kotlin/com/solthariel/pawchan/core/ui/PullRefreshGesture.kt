package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

internal enum class PullRefreshSide { TOP, BOTTOM }

internal enum class PullRefreshReleaseMode { RETRACT, LOAD, FINISH }

internal data class PullRefreshRelease(
    val strain: Float = 0f,
    val mode: PullRefreshReleaseMode = PullRefreshReleaseMode.RETRACT,
    val generation: Int = 0,
)

/** Signed input distance; only unconsumed edge motion is allowed to enter this state. */
internal class PullRefreshGesture {
    var threshold = 1f
        private set
    var distance by mutableFloatStateOf(0f)
        private set
    var side by mutableStateOf(PullRefreshSide.TOP)
        private set
    var requested by mutableStateOf(false)
        private set
    var requestId by mutableIntStateOf(0)
        private set
    var release by mutableStateOf(PullRefreshRelease())
        private set
    var refreshingSeen = false

    val strain: Float
        get() = abs(distance) / threshold

    fun updateHeight(
        height: Int,
        tablet: Boolean,
    ) {
        threshold = pullRefreshThreshold(height.toFloat(), tablet)
    }

    fun consumeClosing(delta: Float): Float {
        if (distance == 0f || delta * distance >= 0f) return 0f
        val consumed = if (distance > 0f) delta.coerceAtLeast(-distance) else delta.coerceAtMost(-distance)
        distance += consumed
        if (distance == 0f) fold(0f, PullRefreshReleaseMode.RETRACT)
        return consumed
    }

    fun consumeEdge(
        delta: Float,
        allowBottom: Boolean,
    ): Float {
        val direction =
            when {
                delta > 0f && distance >= 0f -> PullRefreshSide.TOP
                delta < 0f && distance <= 0f && allowBottom -> PullRefreshSide.BOTTOM
                else -> return 0f
            }
        side = direction
        distance = (distance + delta).coerceIn(-threshold * MAX_PULL_STRAIN, threshold * MAX_PULL_STRAIN)
        return delta
    }

    /** Returns true only once for an armed release; a short/cancelled pull folds back. */
    fun release(busy: Boolean): Boolean {
        val value = strain
        distance = 0f
        if (busy || value < 1f) {
            if (value > 0f) fold(value, PullRefreshReleaseMode.RETRACT)
            return false
        }
        requested = true
        refreshingSeen = false
        requestId++
        fold(value, PullRefreshReleaseMode.LOAD)
        return true
    }

    fun complete() {
        if (!requested) return
        requested = false
        fold(1f, PullRefreshReleaseMode.FINISH)
    }

    private fun fold(
        value: Float,
        mode: PullRefreshReleaseMode,
    ) {
        release = PullRefreshRelease(value, mode, release.generation + 1)
    }
}

internal fun pullRefreshThreshold(
    height: Float,
    tablet: Boolean,
): Float = (height / if (tablet) TABLET_PULL_GAIN else PHONE_PULL_GAIN).coerceAtLeast(1f)

private const val MAX_PULL_STRAIN = 2f
private const val PHONE_PULL_GAIN = 4f
private const val TABLET_PULL_GAIN = 6f
