package com.solthariel.pawchan

import com.solthariel.pawchan.boards.BoardLookupViewModel
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.RecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardLookupViewModelTest {
    @Test
    fun recordsVisitsButLoadsDirectoryOnlyWhenOpenedAndCachesSuccessfulLoad() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val directory = Directory()
                val preferences = Preferences()
                val recent = Visits()
                val model = BoardLookupViewModel(directory, preferences, recent)
                model.visit(defaultBoardAddress())
                advanceUntilIdle()
                assertEquals(0, directory.calls)
                assertEquals(listOf(defaultBoardAddress()), model.state.value.recent)
                model.load()
                model.load()
                advanceUntilIdle()
                model.load()
                advanceUntilIdle()
                assertEquals(1, directory.calls)
                preferences.favoriteBoards.value = setOf(defaultBoardAddress().board)
                advanceUntilIdle()
                assertEquals(preferences.favoriteBoards.value, model.state.value.favorites)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedDirectoryCanRetryAndVisitFailureDoesNotBreakSearch() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val directory = Directory().apply { fail = true }
                val visits = Visits().apply { fail = true }
                val model = BoardLookupViewModel(directory, Preferences(), visits)
                model.visit(defaultBoardAddress())
                model.load()
                advanceUntilIdle()
                assertTrue(model.state.value.failed)
                assertFalse(model.state.value.loading)
                directory.fail = false
                model.load(force = true)
                advanceUntilIdle()
                assertFalse(model.state.value.failed)
                assertEquals(2, directory.calls)
                assertTrue(
                    model.state.value.recent
                        .isEmpty(),
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class Directory : BoardDirectoryRepository {
        var calls = 0
        var fail = false

        override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
            calls++
            return if (fail) {
                DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
            } else {
                DomainOutcome.Success(emptyList())
            }
        }
    }

    private class Visits : RecentBoardsRepository {
        var fail = false

        override suspend fun visit(board: BoardAddress): DomainOutcome<List<BoardAddress>> =
            if (fail) DomainOutcome.Failure(DomainFailure.StorageUnavailable) else DomainOutcome.Success(listOf(board))
    }

    private class Preferences : BoardPreferencesRepository {
        override val favoriteBoards = MutableStateFlow(emptySet<BoardId>())
        override val expandedBoardCategories = MutableStateFlow(emptySet<String>())

        override suspend fun setFavoriteBoard(
            board: BoardId,
            favorite: Boolean,
        ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)

        override suspend fun setBoardCategoryExpanded(
            category: String,
            expanded: Boolean,
        ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)
    }
}
