package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot

/** Main-confined, bounded session cache. Persistent copies are managed separately. */
internal class WorkspaceReadingMemory {
    private val snapshots = linkedMapOf<ThreadAddress, ThreadSnapshot>()
    private val positions = mutableMapOf<ThreadAddress, Pair<Long, Int>>()

    fun remember(snapshot: ThreadSnapshot) {
        snapshots.remove(snapshot.address)
        snapshots[snapshot.address] = snapshot
        while (snapshots.size > MAX_RECENT_SNAPSHOTS) snapshots.remove(snapshots.keys.first())
    }

    fun snapshot(address: ThreadAddress): ThreadSnapshot? = snapshots[address]

    fun position(
        address: ThreadAddress,
        post: Long,
        offset: Int,
    ) {
        positions[address] = post to offset.coerceAtLeast(0)
    }

    fun position(address: ThreadAddress): Pair<Long, Int>? = positions[address]
}

internal fun WorkspaceController.readingPosition(address: ThreadAddress?): Pair<Long, Int>? {
    if (address == null) return null
    val tab =
        state.value.tabs.firstOrNull { it.address == address }
            ?: state.value.copies
                .firstOrNull { it.snapshot.address == address }
                ?.position
    val saved =
        if (readingPreferences.rememberPosition) {
            reading.position(address) ?: tab?.postId?.let { it to tab.offset.coerceAtLeast(0) }
        } else {
            null
        }
    return saved
}

private const val MAX_RECENT_SNAPSHOTS = 6
