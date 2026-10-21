package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.ReadingReturnPoint
import com.solthariel.pawchan.domain.workspace.withReadingPosition
import com.solthariel.pawchan.domain.workspace.withReadingReturn
import com.solthariel.pawchan.domain.workspace.withoutReadingReturn

/** Capture before assigning requestedPost: that override is the destination, not the source. */
internal fun WorkspaceController.requestPost(
    address: ThreadAddress,
    target: Long?,
) {
    val tab =
        state.value.tabs.firstOrNull { it.address == address }
            ?: state.value.copies
                .firstOrNull { it.snapshot.address == address }
                ?.position
    val source = reading.position(address) ?: tab?.postId?.let { it to tab.offset.coerceAtLeast(0) }
    val point =
        source
            ?.takeIf { target != null && (it.first != target || it.second > 0) }
            ?.let { ReadingReturnPoint(it.first, it.second) }
    if (readingPreferences.rememberPosition && point != null) {
        change { it.withReadingReturn(address, point) }
    }
    requestedPost = target?.let { RequestedThreadPost(address, it) }
}

/** Position replacement and anchor removal commit atomically; failed I/O keeps the old anchor. */
internal fun WorkspaceController.completeReadingReturn(
    address: ThreadAddress,
    point: ReadingReturnPoint,
) {
    val remember = readingPreferences.rememberPosition
    if (remember) reading.position(address, point.postId, point.offset)
    change { state ->
        val returned = state.withoutReadingReturn(address, point)
        if (remember) returned.withReadingPosition(address, point.postId, point.offset) else returned
    }
}

/** A pending jump belongs to one full address and is claimed only by its new screen session. */
internal data class RequestedThreadPost(
    val address: ThreadAddress,
    val postId: Long,
)
