package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Records only a successful false-to-true transition, not an optimistic UI click. */
internal class UsageThreadLibrary(
    private val delegate: ThreadLibraryRepository,
    private val usage: UsageStatisticsRepository,
) : ThreadLibraryRepository by delegate {
    private val mutex = Mutex()

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> =
        mutex.withLock {
            val before = delegate.loadEntry(address)
            val result = delegate.setFavorite(address, subject, favorite, updatedAtEpochMillis)
            if (result is DomainOutcome.Success && before is DomainOutcome.Success) {
                if (favorite && before.value?.favorite != true) usage.record(UsageMetric.THREADS_FAVORITED)
            }
            result
        }
}
