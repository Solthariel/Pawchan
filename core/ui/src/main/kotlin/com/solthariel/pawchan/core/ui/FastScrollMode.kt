package com.solthariel.pawchan.core.ui

import kotlin.math.roundToInt

/** CONTENT keeps existing offset navigation; ITEMS snaps only fast-scroll requests to item starts. */
enum class FastScrollMode { CONTENT, ITEMS }

internal fun itemScrollTarget(
    progress: Float,
    itemCount: Int,
    fullyVisible: Int,
): FastScrollTarget =
    when {
        itemCount <= 0 || !progress.isFinite() -> {
            FastScrollTarget(0, 0)
        }

        progress >= 1f -> {
            FastScrollTarget(itemCount - 1, 0)
        }

        else -> {
            val lastStart = (itemCount - fullyVisible.coerceAtLeast(1)).coerceAtLeast(0)
            val index = (lastStart * progress.coerceIn(0f, 1f)).roundToInt().coerceIn(0, itemCount - 1)
            FastScrollTarget(index, 0)
        }
    }

internal fun itemScrollProgress(
    firstIndex: Int,
    itemCount: Int,
    fullyVisible: Int,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Float =
    when {
        !canScrollBackward -> 0f
        !canScrollForward -> 1f
        else -> (firstIndex.toFloat() / (itemCount - fullyVisible.coerceAtLeast(1)).coerceAtLeast(1)).coerceIn(0f, 1f)
    }

internal fun androidx.compose.foundation.lazy.LazyListState.fullyVisibleItems(): Int {
    val layout = layoutInfo
    return layout.visibleItemsInfo.count {
        it.offset >= layout.viewportStartOffset && it.offset + it.size <= layout.viewportEndOffset
    }
}
