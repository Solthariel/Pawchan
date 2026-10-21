package com.solthariel.pawchan.infrastructure.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.ReadingReturnPoint
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.thread.ThreadReadingSession
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.completeReadingReturn
import com.solthariel.pawchan.workspace.readingPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Delegates to the existing owner; no copied cache, new scope or storage queue. */
internal class AppThreadReadingSession(
    private val workspace: WorkspaceController,
) : ThreadReadingSession {
    // The outgoing screen must neither apply nor consume a jump issued to its replacement.
    private var initialRequest = workspace.requestedPost

    override fun restore(address: ThreadAddress): ThreadReadingPosition? =
        workspace.readingPosition(address)?.let { ThreadReadingPosition(it.first, it.second) }

    override fun pendingPost(address: ThreadAddress): Long? = initialRequest?.takeIf { it.address == address }?.postId

    override fun clearPendingPost(address: ThreadAddress) {
        val requested = initialRequest
        if (requested?.address == address) {
            if (workspace.requestedPost === requested) workspace.requestedPost = null
            initialRequest = null
        }
    }

    override fun returnPoint(address: ThreadAddress): Flow<ThreadReadingPosition?> =
        workspace.state
            .map { state ->
                state.marks.readingReturns[address]?.let { ThreadReadingPosition(it.postId, it.offset) }
            }.distinctUntilChanged()

    override fun returned(
        address: ThreadAddress,
        point: ThreadReadingPosition,
    ) {
        workspace.completeReadingReturn(address, ReadingReturnPoint(point.postId, point.offset))
    }

    override fun contentReady(address: ThreadAddress) {
        workspace.recordUsage(
            com.solthariel.pawchan.domain.statistics.UsageMetric.THREADS_OPENED,
            identity = address.toString(),
        )
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
        // A pending debounce from before a return must not overwrite the restored viewport.
        if (workspace.reading.position(address) == (position.postId to position.offset)) {
            workspace.position(address, position.postId, position.offset)
        }
    }
}
