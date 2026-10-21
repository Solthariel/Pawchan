package com.solthariel.pawchan.domain.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress

/** Stable post identity and pixel offset, independent of unread counters and the current viewport. */
data class ReadingReturnPoint(
    val postId: Long,
    val offset: Int,
) {
    init {
        require(postId > 0 && offset >= 0)
    }
}

fun WorkspaceState.withReadingReturn(
    address: ThreadAddress,
    point: ReadingReturnPoint,
): WorkspaceState =
    if (address in marks.readingReturns) {
        this
    } else {
        val retained =
            marks.readingReturns.entries
                .toList()
                .takeLast(SEEN_THREADS_LIMIT - 1)
        copy(marks = marks.copy(readingReturns = retained.associate { it.key to it.value } + (address to point)))
    }

/** A stale return action must not remove a different, newer anchor. */
fun WorkspaceState.withoutReadingReturn(
    address: ThreadAddress,
    expected: ReadingReturnPoint,
): WorkspaceState =
    if (marks.readingReturns[address] == expected) {
        copy(marks = marks.copy(readingReturns = marks.readingReturns - address))
    } else {
        this
    }
