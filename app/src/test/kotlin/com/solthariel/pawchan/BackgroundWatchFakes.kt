package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription

internal class FakeBackgroundThreads(
    private val outcome: DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> = outcome
}

private typealias BackgroundWatchListOutcome = DomainOutcome<List<ThreadWatchSubscription>>

internal class FakeBackgroundWatches(
    vararg initial: ThreadWatchSubscription,
) : ThreadWatchRepository {
    val subscriptions = initial.toMutableList()

    override suspend fun enable(
        address: ThreadAddress,
        subject: String?,
        lastKnownPostId: PostId?,
        enabledAtEpochMillis: Long,
    ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)

    override suspend fun disable(address: ThreadAddress): DomainOutcome<Unit> {
        subscriptions.removeAll { subscription -> subscription.address == address }
        return DomainOutcome.Success(Unit)
    }

    override suspend fun updateCheckpoint(
        address: ThreadAddress,
        lastKnownPostId: PostId,
    ): DomainOutcome<Unit> {
        subscriptions.replaceAll { subscription ->
            if (subscription.shouldAdvance(address, lastKnownPostId)) {
                subscription.copy(lastKnownPostId = lastKnownPostId)
            } else {
                subscription
            }
        }
        return DomainOutcome.Success(Unit)
    }

    override suspend fun load(address: ThreadAddress): DomainOutcome<ThreadWatchSubscription?> =
        DomainOutcome.Success(subscriptions.firstOrNull { it.address == address })

    override suspend fun loadAll(): BackgroundWatchListOutcome = DomainOutcome.Success(subscriptions.toList())
}

private fun ThreadWatchSubscription.shouldAdvance(
    address: ThreadAddress,
    postId: PostId,
): Boolean = this.address == address && (lastKnownPostId?.value ?: 0L) < postId.value
