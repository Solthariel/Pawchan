package com.solthariel.pawchan.data.database

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomThreadLibraryTest {
    @Test
    fun recordsVisitAndPreservesTimestampWhenFavorited() =
        runTest {
            val dao = FakeThreadLibraryDao()
            val repository = RoomThreadLibrary(dao)
            val address = address()

            repository.recordVisit(address, "Тестовый тред", 100L)
            repository.setFavorite(address, "Тестовый тред", favorite = true, 200L)

            val outcome = repository.loadEntry(address) as DomainOutcome.Success
            val entry = requireNotNull(outcome.value)
            assertEquals(100L, entry.lastVisitedAtEpochMillis)
            assertEquals(true, entry.favorite)
        }

    @Test
    fun skipsCorruptedStoredEntries() =
        runTest {
            val corrupted =
                ThreadLibraryEntity(
                    site = "dvach",
                    board = "/invalid",
                    threadId = -1L,
                    subject = "Повреждённая строка",
                    lastVisitedAtEpochMillis = 100L,
                    favorite = true,
                )
            val repository = RoomThreadLibrary(FakeThreadLibraryDao(corrupted))

            val outcome = repository.loadEntries() as DomainOutcome.Success

            assertEquals(emptyList<Any>(), outcome.value)
        }

    @Test
    fun clearingHistoryRemovesNonFavoriteEntries() =
        runTest {
            val repository = RoomThreadLibrary(FakeThreadLibraryDao())
            repository.recordVisit(address(), "History", 100L)

            assertEquals(DomainOutcome.Success(Unit), repository.clearVisitHistory())
            assertEquals(DomainOutcome.Success(emptyList<Any>()), repository.loadEntries())
        }

    @Test
    fun clearingHistoryPreservesFavoriteAndResetsItsVisit() =
        runTest {
            val repository = RoomThreadLibrary(FakeThreadLibraryDao())
            repository.recordVisit(address(), "Favorite", 100L)
            repository.setFavorite(address(), "Favorite", favorite = true, 200L)

            assertEquals(DomainOutcome.Success(Unit), repository.clearVisitHistory())
            val entry = requireNotNull((repository.loadEntry(address()) as DomainOutcome.Success).value)
            assertEquals(true, entry.favorite)
            assertEquals(0L, entry.lastVisitedAtEpochMillis)
        }

    private fun address(): ThreadAddress =
        ThreadAddress(
            board =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("b")),
                ),
            thread = requireNotNull(ThreadId.parse(42L)),
        )
}

private class FakeThreadLibraryDao(
    private var stored: ThreadLibraryEntity? = null,
) : ThreadLibraryDao() {
    override suspend fun insert(entry: ThreadLibraryEntity): Long {
        if (stored == null) {
            stored = entry
            return 1L
        }
        return -1L
    }

    override suspend fun updateVisit(
        site: String,
        board: String,
        threadId: Long,
        subject: String?,
        visitedAt: Long,
    ) {
        stored =
            stored?.copy(
                subject = subject ?: stored?.subject,
                lastVisitedAtEpochMillis = visitedAt,
            )
    }

    override suspend fun updateFavorite(
        site: String,
        board: String,
        threadId: Long,
        subject: String?,
        favorite: Boolean,
    ) {
        stored =
            stored?.copy(
                subject = subject ?: stored?.subject,
                favorite = favorite,
            )
    }

    override suspend fun loadEntry(
        site: String,
        board: String,
        threadId: Long,
    ): ThreadLibraryEntity? = stored

    override suspend fun loadFavorites(): List<ThreadLibraryEntity> = listOfNotNull(stored).filter { it.favorite }

    override suspend fun loadEntries(limit: Int): List<ThreadLibraryEntity> = listOfNotNull(stored)

    override suspend fun deleteNonFavorites() {
        stored = stored?.takeIf { it.favorite }
    }

    override suspend fun resetFavoriteVisits() {
        stored = stored?.let { if (it.favorite) it.copy(lastVisitedAtEpochMillis = 0L) else it }
    }
}
