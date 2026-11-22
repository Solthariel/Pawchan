package com.solthariel.pawchan

import com.solthariel.pawchan.boards.BoardLookupViewModel
import com.solthariel.pawchan.boards.BoardSwipeDirection
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.RecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardLookupViewModelTest {
    @Test
    fun swipeLoadsTheNextForumAndKeepsTheSameBoardCodeWhenAvailable() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val dvach = requireNotNull(SiteId.parse("dvach"))
                val fourChan = requireNotNull(SiteId.parse("4chan"))
                val current = BoardAddress(dvach, requireNotNull(BoardId.parse("b")))
                val next = BoardAddress(fourChan, requireNotNull(BoardId.parse("b")))
                val currentDirectory =
                    Directory().apply {
                        entries = listOf(entry(dvach, "a"), entry(dvach, "b"), entry(dvach, "c"))
                    }
                val nextDirectory =
                    Directory().apply {
                        entries = listOf(entry(fourChan, "a"), entry(fourChan, "b"), entry(fourChan, "c"))
                    }
                val sources =
                    listOf(
                        ForumBoardSource(dvach, "2ch", currentDirectory),
                        ForumBoardSource(fourChan, "4chan", nextDirectory),
                    )
                val model = BoardLookupViewModel(currentDirectory, Preferences(), null, forumSources = sources)
                var selected: BoardAddress? = null

                model.selectAdjacentForum(current, BoardSwipeDirection.NEXT) { selected = it }
                advanceUntilIdle()

                assertEquals(next, selected)
                assertEquals(0, currentDirectory.calls)
                assertEquals(1, nextDirectory.calls)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun swipeUsesFirstBoardWhenTheNextForumHasNoMatchingBoardCode() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val dvach = requireNotNull(SiteId.parse("dvach"))
                val fourChan = requireNotNull(SiteId.parse("4chan"))
                val current = BoardAddress(dvach, requireNotNull(BoardId.parse("missing")))
                val next = BoardAddress(fourChan, requireNotNull(BoardId.parse("a")))
                val currentDirectory = Directory()
                val nextDirectory =
                    Directory().apply {
                        entries = listOf(entry(fourChan, "z"), entry(fourChan, "a"))
                    }
                val sources =
                    listOf(
                        ForumBoardSource(dvach, "2ch", currentDirectory),
                        ForumBoardSource(fourChan, "4chan", nextDirectory),
                    )
                val model =
                    BoardLookupViewModel(
                        currentDirectory,
                        Preferences(),
                        null,
                        forumSources = sources,
                    )
                var selected: BoardAddress? = null

                model.selectAdjacentForum(current, BoardSwipeDirection.NEXT) { selected = it }
                advanceUntilIdle()

                assertEquals(next, selected)
                assertEquals(0, currentDirectory.calls)
                assertEquals(1, nextDirectory.calls)
            } finally {
                Dispatchers.resetMain()
            }
        }

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
                preferences.favoriteBoards.value = setOf(defaultBoardAddress())
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
                assertTrue(model.state.value.quickBoardsReady)
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

    @Test
    fun quickBoardsWaitForRecentVisitsInsteadOfPresentingAFalseEmptyState() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val deferred = CompletableDeferred<DomainOutcome<List<BoardAddress>>>()
                val visits =
                    object : RecentBoardsRepository {
                        override suspend fun visit(board: BoardAddress) = deferred.await()
                    }
                val model = BoardLookupViewModel(Directory(), Preferences(), visits)
                model.visit(defaultBoardAddress())
                runCurrent()
                assertFalse(model.state.value.quickBoardsReady)
                deferred.complete(DomainOutcome.Success(listOf(defaultBoardAddress())))
                advanceUntilIdle()
                assertTrue(model.state.value.quickBoardsReady)
                assertEquals(listOf(defaultBoardAddress()), model.state.value.recent)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedLaterVisitKeepsThePreviouslyLoadedQuickBoards() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val visits = Visits()
                val model = BoardLookupViewModel(Directory(), Preferences(), visits)
                model.visit(defaultBoardAddress())
                advanceUntilIdle()
                visits.fail = true
                model.visit(defaultBoardAddress())
                advanceUntilIdle()
                assertTrue(model.state.value.quickBoardsReady)
                assertEquals(listOf(defaultBoardAddress()), model.state.value.recent)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class Directory : BoardDirectoryRepository {
        var calls = 0
        var fail = false
        var entries = emptyList<BoardDirectoryEntry>()

        override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
            calls++
            return if (fail) {
                DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
            } else {
                DomainOutcome.Success(entries)
            }
        }
    }

    private class Visits : RecentBoardsRepository {
        var fail = false

        override suspend fun visit(board: BoardAddress): DomainOutcome<List<BoardAddress>> =
            if (fail) DomainOutcome.Failure(DomainFailure.StorageUnavailable) else DomainOutcome.Success(listOf(board))
    }

    private class Preferences : BoardPreferencesRepository {
        override val favoriteBoards = MutableStateFlow(emptySet<BoardAddress>())
        override val expandedBoardCategories = MutableStateFlow(emptySet<String>())

        override suspend fun setFavoriteBoard(
            board: BoardAddress,
            favorite: Boolean,
        ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)

        override suspend fun setBoardCategoryExpanded(
            category: String,
            expanded: Boolean,
        ): DomainOutcome<Unit> = DomainOutcome.Success(Unit)
    }

    private fun entry(
        site: SiteId,
        board: String,
    ) = BoardDirectoryEntry(
        address = BoardAddress(site, requireNotNull(BoardId.parse(board))),
        title = board,
        category = "Тематика",
        postsPerHour = 0,
        postingEnabled = true,
    )
}
