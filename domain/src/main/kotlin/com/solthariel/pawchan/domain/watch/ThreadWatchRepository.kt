package com.solthariel.pawchan.domain.watch

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress

data class ThreadWatchSubscription(
    val address: ThreadAddress,
    val subject: String?,
    val lastKnownPostId: PostId?,
    val enabledAtEpochMillis: Long,
)

interface ThreadWatchRepository {
    suspend fun enable(
        address: ThreadAddress,
        subject: String?,
        lastKnownPostId: PostId?,
        enabledAtEpochMillis: Long,
    ): DomainOutcome<Unit>

    suspend fun disable(address: ThreadAddress): DomainOutcome<Unit>

    suspend fun updateCheckpoint(
        address: ThreadAddress,
        lastKnownPostId: PostId,
    ): DomainOutcome<Unit>

    suspend fun load(address: ThreadAddress): DomainOutcome<ThreadWatchSubscription?>

    suspend fun loadAll(): DomainOutcome<List<ThreadWatchSubscription>>
}
