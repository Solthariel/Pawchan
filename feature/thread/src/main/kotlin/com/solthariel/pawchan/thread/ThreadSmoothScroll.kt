package com.solthariel.pawchan.thread

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Прокрутка к посту как `ListViewUtils.smoothScrollToPosition` в Dashchan: если цель дальше порога
 * ([jumpThreshold] постов, `screenHeightDp / 40`), сначала мгновенный перескок, чтобы до неё оставался
 * порог; затем доводка с постоянной скоростью `LinearSmoothScroller` (25 мс на дюйм), пост встаёт к
 * верхнему краю. Если пост в самом конце треда, лента плавно доходит до низа и не упирается рывком.
 */
internal class ThreadSmoothScroll(
    private val jumpThreshold: Int,
    private val pixelsPerMillisecond: Float,
) {
    suspend fun scrollTo(
        list: LazyListState,
        index: Int,
    ) {
        val first = list.firstVisibleItemIndex
        when {
            index > first + jumpThreshold -> list.scrollToItem(index - jumpThreshold)
            index < first - jumpThreshold -> list.scrollToItem(index + jumpThreshold)
        }
        repeat(MAX_STEPS) {
            val step = nextStep(list.layoutInfo, index, list.firstVisibleItemIndex) ?: return
            val duration = (abs(step.distance) / pixelsPerMillisecond).roundToInt().coerceAtLeast(1)
            val easing = if (step.final && step.clamped) LinearOutSlowInEasing else LinearEasing
            val consumed = list.animateScrollBy(step.distance, tween(duration, easing = easing))
            if (step.final || abs(consumed) < abs(step.distance) - 1f) return
        }
    }

    private fun nextStep(
        info: LazyListLayoutInfo,
        index: Int,
        first: Int,
    ): ScrollStep? {
        val target = info.visibleItemsInfo.firstOrNull { it.index == index }
        if (target == null) {
            // Цель ещё не разложена: доводка идёт той же скоростью экранами, пока пост не появится.
            val viewport = info.viewportSize.height.toFloat()
            return ScrollStep(if (index > first) viewport else -viewport, final = false, clamped = false)
        }
        val toTop = target.offset.toFloat()
        val toEnd = info.remainingToEnd()
        val clamped = toEnd != null && toTop > toEnd
        val distance = if (clamped) requireNotNull(toEnd) else toTop
        return ScrollStep(distance, final = true, clamped = clamped).takeIf { abs(distance) >= 1f }
    }

    private class ScrollStep(
        val distance: Float,
        val final: Boolean,
        val clamped: Boolean,
    )
}

/** Сколько ещё можно прокрутить вниз, если последний пост уже на экране; иначе неизвестно. */
private fun LazyListLayoutInfo.remainingToEnd(): Float? {
    val last = visibleItemsInfo.lastOrNull()?.takeIf { it.index == totalItemsCount - 1 }
    return last?.let { (it.offset + it.size + afterContentPadding - viewportSize.height).coerceAtLeast(0).toFloat() }
}

@Composable
internal fun rememberThreadSmoothScroll(): ThreadSmoothScroll {
    val density = LocalDensity.current.density
    val screenHeight = (LocalWindowInfo.current.containerSize.height / density).toInt()
    return remember(screenHeight, density) {
        ThreadSmoothScroll(
            jumpThreshold = (screenHeight / JUMP_THRESHOLD_DIVIDER).coerceAtLeast(1),
            pixelsPerMillisecond = density * DPI_PER_DENSITY / MILLISECONDS_PER_INCH,
        )
    }
}

/** Как `ListViewUtils.getScrollJumpThreshold` в Dashchan. */
private const val JUMP_THRESHOLD_DIVIDER = 40

/** `LinearSmoothScroller.MILLISECONDS_PER_INCH`. */
private const val MILLISECONDS_PER_INCH = 25f
private const val DPI_PER_DENSITY = 160f
private const val MAX_STEPS = 64
