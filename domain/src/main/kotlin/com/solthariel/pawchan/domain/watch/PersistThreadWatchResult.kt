package com.solthariel.pawchan.domain.watch

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

/** Persistence only; callers publish notifications before invoking this operation. */
class PersistThreadWatchResult(
    private val watches: ThreadWatchRepository,
) {
    suspend operator fun invoke(
        address: ThreadAddress,
        result: ThreadWatchResult,
    ): DomainOutcome<Unit> {
        val checkpoint =
            when (result) {
                is ThreadWatchResult.Unchanged -> result.checkpoint
                is ThreadWatchResult.NewPosts -> result.checkpoint
                is ThreadWatchResult.Closed -> result.checkpoint
                ThreadWatchResult.Deleted -> null
            }
        val saved = checkpoint?.let { watches.updateCheckpoint(address, it) } ?: DomainOutcome.Success(Unit)
        val terminal = result is ThreadWatchResult.Closed || result == ThreadWatchResult.Deleted
        val stopped = if (terminal) watches.disable(address) else DomainOutcome.Success(Unit)
        return if (saved is DomainOutcome.Failure) saved else stopped
    }
}
