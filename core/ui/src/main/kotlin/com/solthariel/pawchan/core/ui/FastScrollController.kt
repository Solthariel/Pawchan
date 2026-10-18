package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class FastScrollController(
    private val state: LazyListState,
    private val scope: CoroutineScope,
) {
    private var endJob: Job? = null

    fun request(
        progress: Float,
        geometry: FastScrollGeometry,
        mode: FastScrollMode = FastScrollMode.CONTENT,
    ) {
        if (geometry.totalItems <= 0 || !progress.isFinite()) return
        if (progress >= 1f) {
            requestEnd(geometry.totalItems - 1)
            return
        }
        endJob?.cancel()
        endJob = null
        val target =
            if (mode == FastScrollMode.ITEMS) {
                itemScrollTarget(progress, geometry.totalItems, state.fullyVisibleItems())
            } else {
                geometry.target(progress)
            }
        // Pointer updates are coalesced into remeasurement, not a coroutine per pixel.
        state.requestScrollToItem(target.index, target.offset)
    }

    private fun requestEnd(lastIndex: Int) {
        if (endJob?.isActive == true || !state.canScrollForward) return
        endJob =
            scope.launch {
                // Measure the real last row first. It may be taller than the viewport.
                state.scrollToItem(lastIndex)
                val layout = state.layoutInfo
                val lastItem = layout.visibleItemsInfo.lastOrNull { it.index == lastIndex } ?: return@launch
                // Consume the remaining bottom of that row and the actual content padding.
                // LazyColumn clamps this overshoot to its true end, including tall rows.
                state.scrollBy(lastItem.size.toFloat() + layout.beforeContentPadding + layout.afterContentPadding)
            }
    }
}

internal fun LazyListState.fastScrollGeometry(itemSizes: FastScrollItemSizes? = null): FastScrollGeometry {
    val layout = layoutInfo
    var visibleHeight = 0L
    for (item in layout.visibleItemsInfo) {
        visibleHeight += item.size
    }
    val visibleCount = layout.visibleItemsInfo.size.coerceAtLeast(1)
    return FastScrollGeometry(
        totalItems = layout.totalItemsCount,
        viewportHeight = layout.viewportSize.height,
        averageItemHeight = visibleHeight.toFloat() / visibleCount,
        itemSpacing = layout.mainAxisItemSpacing,
        contentPadding = FastScrollContentPadding(layout.beforeContentPadding, layout.afterContentPadding),
        itemSizes =
            itemSizes?.takeIf {
                it.itemCount == layout.totalItemsCount && layout.mainAxisItemSpacing == 0
            },
    )
}
