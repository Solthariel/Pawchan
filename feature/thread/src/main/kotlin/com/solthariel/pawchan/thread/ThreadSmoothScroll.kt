package com.solthariel.pawchan.thread

import androidx.compose.foundation.lazy.LazyListState

internal suspend fun LazyListState.scrollToThreadPost(index: Int) {
    val count = layoutInfo.totalItemsCount
    if (index < 0 || count == 0) return
    animateScrollToItem(index.coerceAtMost(count - 1))
}
