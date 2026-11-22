package com.solthariel.pawchan.thread

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Brief exposure during scroll or a covered window must not consume the reply. */
internal suspend fun collectVisibleReplies(
    visible: Flow<Set<Long>>,
    unread: Flow<Set<Long>>,
    onRead: (Set<Long>) -> Unit,
) {
    combine(visible, unread) { shown, pending -> shown intersect pending }
        .distinctUntilChanged()
        .collectLatest { ids ->
            if (ids.isNotEmpty()) {
                delay(READ_DWELL_MILLIS)
                onRead(ids)
            }
        }
}

private const val READ_DWELL_MILLIS = 500L

internal fun replyIsVisible(
    offset: Int,
    size: Int,
    viewportStart: Int,
    viewportEnd: Int,
    minimumPixels: Int,
): Boolean {
    if (size <= 0 || minimumPixels <= 0 || viewportEnd <= viewportStart) return false
    val visible = minOf(offset + size, viewportEnd) - maxOf(offset, viewportStart)
    return visible >= minOf(size, minimumPixels)
}
