package com.solthariel.pawchan

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription

internal class IntegrationThreadLibrary : ThreadLibraryRepository {
    var visits: Int = 0
        private set
    var favorite: Boolean = false
        private set

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        visits += 1
        return DomainOutcome.Success(Unit)
    }

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        this.favorite = favorite
        return DomainOutcome.Success(Unit)
    }

    override suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?> =
        DomainOutcome.Success(
            ThreadLibraryEntry(
                address = address,
                subject = "Тестовый тред",
                lastVisitedAtEpochMillis = 1L,
                favorite = favorite,
            ),
        )

    override suspend fun loadEntries(): DomainOutcome<List<ThreadLibraryEntry>> = DomainOutcome.Success(emptyList())
}

private typealias WatchLoadOutcome = DomainOutcome<ThreadWatchSubscription?>
private typealias WatchListOutcome = DomainOutcome<List<ThreadWatchSubscription>>

internal class IntegrationWatchRepository : ThreadWatchRepository {
    var subscription: ThreadWatchSubscription? = null

    override suspend fun enable(
        address: ThreadAddress,
        subject: String?,
        lastKnownPostId: PostId?,
        enabledAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        subscription =
            ThreadWatchSubscription(address, subject, lastKnownPostId, enabledAtEpochMillis)
        return DomainOutcome.Success(Unit)
    }

    override suspend fun disable(address: ThreadAddress): DomainOutcome<Unit> {
        subscription = null
        return DomainOutcome.Success(Unit)
    }

    override suspend fun updateCheckpoint(
        address: ThreadAddress,
        lastKnownPostId: PostId,
    ): DomainOutcome<Unit> {
        subscription = subscription?.copy(lastKnownPostId = lastKnownPostId)
        return DomainOutcome.Success(Unit)
    }

    override suspend fun load(address: ThreadAddress): WatchLoadOutcome = DomainOutcome.Success(subscription)

    override suspend fun loadAll(): WatchListOutcome = DomainOutcome.Success(listOfNotNull(subscription))
}
