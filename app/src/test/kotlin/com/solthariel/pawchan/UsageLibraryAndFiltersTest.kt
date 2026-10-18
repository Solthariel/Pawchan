package com.solthariel.pawchan

import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.infrastructure.UsageFilterStatistics
import com.solthariel.pawchan.infrastructure.UsageThreadLibrary
import com.solthariel.pawchan.navigation.threadAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageLibraryAndFiltersTest {
    private val address = requireNotNull(threadAddress("dvach", "b", 42))

    @Test fun repeatedFavoriteWritesCountOnlySuccessfulTransitions() =
        runTest {
            val stats = UsageEvents()
            val library = UsageThreadLibrary(UsageLibraryFake(), stats)
            library.setFavorite(address, "title", true, 1)
            library.setFavorite(address, "title", true, 2)
            library.setFavorite(address, "title", false, 3)
            library.setFavorite(address, "title", true, 4)
            val expected = listOf(UsageMetric.THREADS_FAVORITED, UsageMetric.THREADS_FAVORITED)
            assertEquals(expected, stats.events.map { it.first })
        }

    @Test fun rejectedFavoriteWriteDoesNotIncreaseCounter() =
        runTest {
            val stats = UsageEvents()
            val library = UsageThreadLibrary(UsageLibraryFake(fail = true), stats)
            assertTrue(library.setFavorite(address, null, true, 1) is DomainOutcome.Failure)
            assertTrue(stats.events.isEmpty())
        }

    @Test fun autoHideCountsOriginalPostsButNotRepliesOrMultipleRules() =
        runTest {
            val stats = UsageEvents()
            val filters = UsageFilterStatistics(FilterUsageFake(), stats)
            filters.record(
                listOf(
                    ContentFilterHit("a", address, 42, true),
                    ContentFilterHit("b", address, 42, true),
                    ContentFilterHit("c", address, 43, false),
                ),
            )
            assertEquals(listOf(UsageMetric.THREADS_AUTO_HIDDEN to address.toString()), stats.events)
        }

    @Test fun failedFilterStorageDoesNotInventUsage() =
        runTest {
            val stats = UsageEvents()
            val filters = UsageFilterStatistics(FilterUsageFake(fail = true), stats)
            assertTrue(filters.record(listOf(ContentFilterHit("a", address, 42, true))) is DomainOutcome.Failure)
            assertTrue(stats.events.isEmpty())
        }
}

private class UsageEvents : UsageStatisticsRepository {
    override val days = MutableStateFlow(emptyList<UsageDay>())
    override val available = MutableStateFlow(true)
    val events = mutableListOf<Pair<UsageMetric, String?>>()

    override suspend fun record(
        metric: UsageMetric,
        amount: Long,
        identity: String?,
    ) {
        if (amount > 0) events += metric to identity
    }

    override suspend fun reset() = events.clear()
}

private class UsageLibraryFake(
    private val fail: Boolean = false,
) : ThreadLibraryRepository {
    private var entry: ThreadLibraryEntry? = null

    override suspend fun loadEntry(address: ThreadAddress) = DomainOutcome.Success(entry)

    override suspend fun loadEntries() = DomainOutcome.Success(listOfNotNull(entry))

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ) = DomainOutcome.Success(Unit)

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        if (fail) return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        entry = ThreadLibraryEntry(address, subject, updatedAtEpochMillis, favorite)
        return DomainOutcome.Success(Unit)
    }
}

private class FilterUsageFake(
    private val fail: Boolean = false,
) : ContentFilterStatisticsRepository {
    override val statistics = MutableStateFlow(ContentFilterStatistics())

    override suspend fun record(hits: List<ContentFilterHit>): DomainOutcome<Unit> =
        if (fail) DomainOutcome.Failure(DomainFailure.StorageUnavailable) else DomainOutcome.Success(Unit)

    override suspend fun reset(): DomainOutcome<Unit> = DomainOutcome.Success(Unit)
}
