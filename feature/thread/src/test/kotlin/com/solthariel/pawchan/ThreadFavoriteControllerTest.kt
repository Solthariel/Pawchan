package com.solthariel.pawchan

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.PostContentAccess
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadFavoriteController
import com.solthariel.pawchan.thread.ThreadUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadFavoriteControllerTest {
    @Test fun membershipCanBeAddedRemovedAndAddedAgain() =
        runTest {
            val repository = FavoriteRepository()
            var state: ThreadUiState = content(false)
            val controller =
                ThreadFavoriteController(
                    this,
                    repository.address,
                    repository,
                    PostContentAccess({ state }, { state = it }),
                    { 1 },
                )
            controller.set(true)
            advanceUntilIdle()
            assertTrue((state as ThreadUiState.Content).isFavorite)
            controller.set(false)
            advanceUntilIdle()
            assertFalse((state as ThreadUiState.Content).isFavorite)
            controller.set(true)
            advanceUntilIdle()
            assertTrue(repository.favorite)
            assertEquals(listOf(true, false, true), repository.writes)
        }

    @Test fun failedRemovalRollsBackWithoutDeletingHistory() =
        runTest {
            val repository = FavoriteRepository(favorite = true, failWrites = true)
            var state: ThreadUiState = content(true)
            val controller =
                ThreadFavoriteController(
                    this,
                    repository.address,
                    repository,
                    PostContentAccess({ state }, { state = it }),
                    { 1 },
                )
            controller.set(false)
            advanceUntilIdle()
            assertTrue((state as ThreadUiState.Content).isFavorite)
            assertTrue(repository.favorite)
            assertEquals("subject", (state as ThreadUiState.Content).subject)
        }

    @Test fun resumingSynchronizesExternalRemovalAndDecoratesLateSnapshots() =
        runTest {
            val repository = FavoriteRepository(favorite = false)
            var state: ThreadUiState = content(true)
            val controller =
                ThreadFavoriteController(
                    this,
                    repository.address,
                    repository,
                    PostContentAccess({ state }, { state = it }),
                    { 1 },
                )
            controller.refresh()
            advanceUntilIdle()
            assertFalse((state as ThreadUiState.Content).isFavorite)
            assertFalse((controller.decorate(content(true)) as ThreadUiState.Content).isFavorite)
            assertTrue(repository.writes.isEmpty())
        }

    private fun content(favorite: Boolean): ThreadUiState.Content =
        ThreadUiState.Content(
            42,
            "subject",
            emptyList(),
            ThreadContentFlags(false),
            ThreadContentPresentation(favorite),
            ThreadContentStatistics(),
            defaultThreadAddress(42),
        )
}

private class FavoriteRepository(
    var favorite: Boolean = false,
    private val failWrites: Boolean = false,
) : ThreadLibraryRepository {
    val address = defaultThreadAddress(42)
    val writes = mutableListOf<Boolean>()

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        require(address == this.address)
        if (failWrites) return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        writes.add(favorite)
        this.favorite = favorite
        return DomainOutcome.Success(Unit)
    }

    override suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?> =
        DomainOutcome.Success(ThreadLibraryEntry(address, "subject", 1, favorite))

    override suspend fun loadEntries(): DomainOutcome<List<ThreadLibraryEntry>> = DomainOutcome.Success(emptyList())

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)
}
