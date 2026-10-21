package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class ThreadReadingPosition(
    val postId: Long,
    val offset: Int,
)

/** Borrowed app persistence; the screen owns only collection of visible positions. */
interface ThreadReadingSession {
    fun restore(address: ThreadAddress): ThreadReadingPosition?

    fun pendingPost(address: ThreadAddress): Long? = null

    fun clearPendingPost(address: ThreadAddress) = Unit

    fun returnPoint(address: ThreadAddress): Flow<ThreadReadingPosition?> = flowOf(null)

    fun returned(
        address: ThreadAddress,
        point: ThreadReadingPosition,
    ) = Unit

    fun contentReady(address: ThreadAddress)

    fun visible(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    )

    fun persist(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    )
}

internal fun threadListAnchor(
    positions: Map<Long, Int>?,
    position: ThreadReadingPosition?,
): Pair<Int, Int> = if (position == null) 0 to 0 else (positions?.get(position.postId) ?: 0) to position.offset
