package com.solthariel.pawchan.watch

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.PersistThreadWatchResult
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchResult
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription

internal enum class BackgroundWatchRunResult {
    SUCCESS,
    RETRY,
}

internal fun interface ThreadWatchEventSink {
    fun onNewPosts(
        subscription: ThreadWatchSubscription,
        count: Int,
    )
}

internal class ThreadWatchBackgroundRunner(
    private val watches: ThreadWatchRepository,
    private val checkWatchedThread: CheckWatchedThread,
    private val events: ThreadWatchEventSink = ThreadWatchEventSink { _, _ -> },
) {
    suspend fun run(): BackgroundWatchRunResult {
        val subscriptions =
            when (val outcome = watches.loadAll()) {
                is DomainOutcome.Success -> outcome.value
                is DomainOutcome.Failure -> return BackgroundWatchRunResult.RETRY
            }
        var failed = false
        subscriptions.forEach { subscription ->
            if (!process(subscription)) {
                failed = true
            }
        }
        return if (failed) {
            BackgroundWatchRunResult.RETRY
        } else {
            BackgroundWatchRunResult.SUCCESS
        }
    }

    private suspend fun process(subscription: ThreadWatchSubscription): Boolean =
        when (val outcome = checkWatchedThread(subscription)) {
            is DomainOutcome.Success -> persist(subscription, outcome.value)
            is DomainOutcome.Failure -> false
        }

    private suspend fun persist(
        subscription: ThreadWatchSubscription,
        result: ThreadWatchResult,
    ): Boolean {
        val newPosts =
            when (result) {
                is ThreadWatchResult.NewPosts -> result.posts.size
                is ThreadWatchResult.Closed -> result.posts.size.takeIf { it > 0 }
                else -> null
            }
        newPosts?.let { events.onNewPosts(subscription, it) }
        return PersistThreadWatchResult(watches)(subscription.address, result) is DomainOutcome.Success
    }
}
