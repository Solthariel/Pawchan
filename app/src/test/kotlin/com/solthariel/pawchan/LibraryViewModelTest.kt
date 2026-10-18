package com.solthariel.pawchan

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.library.LibraryUiState
import com.solthariel.pawchan.library.LibraryViewModel
import com.solthariel.pawchan.navigation.threadAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Test
    fun loadsHistoryAndTogglesFavorite() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val address = requireNotNull(threadAddress("dvach", "po", 42L))
                val repository = FakeLibraryRepository(entry(address, favorite = false))
                val viewModel = LibraryViewModel(repository) { 200L }
                advanceUntilIdle()

                val loaded = (viewModel.state.value as LibraryUiState.Content).entries.single()
                assertEquals(42L, loaded.threadId)
                assertEquals(false, loaded.favorite)

                viewModel.setFavorite(loaded, true)
                advanceUntilIdle()

                val updated = (viewModel.state.value as LibraryUiState.Content).entries.single()
                assertEquals(true, updated.favorite)
                assertEquals(true, repository.favorite)
                assertEquals(address, repository.favoriteAddress)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun removalKeepsTheHistoryEntryAndCannotBeUndoneByARepeatedStaleAction() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = requireNotNull(threadAddress("dvach", "po", 42L))
                val repository = FakeLibraryRepository(entry(address, favorite = true))
                val model = LibraryViewModel(repository)
                advanceUntilIdle()
                val original = (model.state.value as LibraryUiState.Content).entries.single()
                model.setFavorite(original, false)
                advanceUntilIdle()
                model.setFavorite(original, false)
                advanceUntilIdle()
                val result = model.state.value as LibraryUiState.Content
                assertEquals(0, result.favorites.size)
                assertEquals(1, result.entries.size)
                assertEquals(original.lastVisitedAtEpochMillis, result.entries.single().lastVisitedAtEpochMillis)
                assertEquals(false, repository.favorite)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedRemovalRestoresTheFavoriteEntry() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = requireNotNull(threadAddress("dvach", "po", 42L))
                val repository = FakeLibraryRepository(entry(address, favorite = true), failWrites = true)
                val model = LibraryViewModel(repository)
                advanceUntilIdle()
                model.setFavorite((model.state.value as LibraryUiState.Content).entries.single(), false)
                advanceUntilIdle()
                assertEquals(1, (model.state.value as LibraryUiState.Content).favorites.size)
                assertEquals(true, repository.favorite)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun entry(
        address: ThreadAddress,
        favorite: Boolean,
    ): ThreadLibraryEntry =
        ThreadLibraryEntry(
            address = address,
            subject = "Тестовый тред",
            lastVisitedAtEpochMillis = 100L,
            favorite = favorite,
        )
}

private class FakeLibraryRepository(
    private val entry: ThreadLibraryEntry,
    private val failWrites: Boolean = false,
) : ThreadLibraryRepository {
    var favorite: Boolean = entry.favorite
        private set
    var favoriteAddress: ThreadAddress? = null
        private set

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        if (failWrites) return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        favoriteAddress = address
        this.favorite = favorite
        return DomainOutcome.Success(Unit)
    }

    override suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?> =
        DomainOutcome.Success(entry.copy(favorite = favorite))

    override suspend fun loadEntries(): DomainOutcome<List<ThreadLibraryEntry>> =
        DomainOutcome.Success(listOf(entry.copy(favorite = favorite)))
}
