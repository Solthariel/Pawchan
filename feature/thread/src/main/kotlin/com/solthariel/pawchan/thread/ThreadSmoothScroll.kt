package com.solthariel.pawchan.thread

import androidx.compose.foundation.lazy.LazyListState

internal suspend fun LazyListState.scrollToThreadPost(index: Int) {
    val info = layoutInfo
    if (index < 0 || info.totalItemsCount == 0) return
    val target = index.coerceAtMost(info.totalItemsCount - 1)
    val visible = info.visibleItemsInfo
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(0)
    if (viewport > 0 && visible.isNotEmpty()) {
        val approach =
            when {
                target > visible.last().index -> -viewport
                target < visible.first().index -> viewport
                else -> null
            }
        if (approach != null) scrollToItem(target, approach)
    }
    val remaining = layoutInfo.totalItemsCount
    if (remaining > 0) animateScrollToItem(target.coerceAtMost(remaining - 1))
}
