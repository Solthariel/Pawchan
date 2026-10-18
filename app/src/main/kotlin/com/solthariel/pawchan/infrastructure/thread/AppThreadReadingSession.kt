package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.thread.ThreadReadingSession
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.readingPosition

/** Delegates to the existing owner; no copied cache, new scope or storage queue. */
internal class AppThreadReadingSession(
    private val workspace: WorkspaceController,
) : ThreadReadingSession {
    private val seen = mutableSetOf<String>()
    private val opened = mutableSetOf<ThreadAddress>()

    override fun viewed(
        address: ThreadAddress,
        postIds: List<Long>,
    ) {
        val added = postIds.count { seen.add("$address/$it") }
        workspace.recordUsage(com.solthariel.pawchan.domain.statistics.UsageMetric.POSTS_VIEWED, added.toLong())
    }

    override fun restore(address: ThreadAddress): ThreadReadingPosition? =
        workspace.readingPosition(address)?.let { ThreadReadingPosition(it.first, it.second) }

    override fun contentReady(address: ThreadAddress) {
        workspace.requestedPost = null
        if (opened.add(address)) {
            workspace.recordUsage(com.solthariel.pawchan.domain.statistics.UsageMetric.THREADS_OPENED)
        }
    }

    override fun visible(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) {
        if (workspace.readingPreferences.rememberPosition) {
            workspace.reading.position(address, position.postId, position.offset)
        }
    }

    override fun persist(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) {
        workspace.position(address, position.postId, position.offset)
    }
}
