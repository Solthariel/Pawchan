package com.solthariel.pawchan.thread.watch

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.PersistThreadWatchResult
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchResult
import com.solthariel.pawchan.thread.ThreadError
import com.solthariel.pawchan.thread.toThreadError

internal data class ForegroundThreadWatchUpdate(
    val snapshot: ThreadSnapshot?,
    val poll: ThreadWatcherPoll,
)

internal class ForegroundThreadWatch(
    private val address: ThreadAddress,
    private val watches: ThreadWatchRepository,
    private val checkWatchedThread: CheckWatchedThread,
) {
    suspend fun check(): ForegroundThreadWatchUpdate? =
        when (val subscription = watches.load(address)) {
            is DomainOutcome.Success -> {
                subscription.value?.let { checkWatchedThread(it).toUpdate() }
            }

            is DomainOutcome.Failure -> {
                ForegroundThreadWatchUpdate(
                    snapshot = null,
                    poll = ThreadWatcherPoll(newPosts = 0, error = ThreadError.STORAGE),
                )
            }
        }

    private suspend fun DomainOutcome<ThreadWatchResult>.toUpdate(): ForegroundThreadWatchUpdate =
        when (this) {
            is DomainOutcome.Success -> {
                handle(value)
            }

            is DomainOutcome.Failure -> {
                ForegroundThreadWatchUpdate(
                    snapshot = null,
                    poll = ThreadWatcherPoll(newPosts = 0, error = reason.toThreadError()),
                )
            }
        }

    private suspend fun handle(result: ThreadWatchResult): ForegroundThreadWatchUpdate {
        val snapshot =
            when (result) {
                is ThreadWatchResult.Unchanged -> result.snapshot
                is ThreadWatchResult.NewPosts -> result.snapshot
                is ThreadWatchResult.Closed -> result.snapshot
                ThreadWatchResult.Deleted -> null
            }
        val newPosts =
            when (result) {
                is ThreadWatchResult.NewPosts -> result.posts.size
                is ThreadWatchResult.Closed -> result.posts.size
                else -> 0
            }
        val stopReason =
            when (result) {
                is ThreadWatchResult.Closed -> ThreadWatcherStopReason.CLOSED
                ThreadWatchResult.Deleted -> ThreadWatcherStopReason.DELETED
                else -> null
            }
        val saved = PersistThreadWatchResult(watches)(address, result)
        return ForegroundThreadWatchUpdate(
            snapshot,
            ThreadWatcherPoll(newPosts, if (saved is DomainOutcome.Failure) ThreadError.STORAGE else null, stopReason),
        )
    }
}
