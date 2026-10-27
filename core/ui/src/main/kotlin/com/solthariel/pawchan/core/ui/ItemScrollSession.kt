package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Measures only the tail viewport and requests restoration before applying the requested post.
 * The mapping is frozen for the entire gesture, never derived from a changing visible-row count.
 */
internal class ItemScrollSession(
    private val state: LazyListState,
    private val scope: CoroutineScope,
) {
    var anchors by mutableStateOf<ItemScrollAnchors?>(null)
        private set
    private var measuring: Job? = null
    private var pending: Float? = null

    fun begin() {
        if (measuring?.isActive == true || state.layoutInfo.totalItemsCount == 0) return
        pending = null
        measuring =
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                val original = FastScrollTarget(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
                try {
                    val lastIndex = state.layoutInfo.totalItemsCount - 1
                    state.scrollToItem(lastIndex)
                    val layout = state.layoutInfo
                    val last = layout.visibleItemsInfo.lastOrNull { it.index == lastIndex }
                    if (last != null) {
                        val overshoot = last.size.toFloat() + layout.beforeContentPadding + layout.afterContentPadding
                        state.scrollBy(overshoot)
                    }
                    anchors =
                        ItemScrollAnchors(
                            FastScrollTarget(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset),
                        )
                    state.scrollToItem(original.index, original.offset)
                } finally {
                    // requestScrollToItem also restores position if the measuring coroutine is cancelled.
                    state.requestScrollToItem(original.index, original.offset)
                }
                pending?.let(::apply)
            }
    }

    fun request(progress: Float) {
        if (!progress.isFinite()) return
        pending = progress
        if (measuring?.isActive != true) apply(progress)
    }

    private fun apply(progress: Float) {
        val target = anchors?.target(progress) ?: return
        state.requestScrollToItem(target.index, target.offset)
    }
}
