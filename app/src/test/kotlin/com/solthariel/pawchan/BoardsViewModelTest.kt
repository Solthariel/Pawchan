package com.solthariel.pawchan

import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.BoardsViewModel
import com.solthariel.pawchan.boards.FAVORITES_CATEGORY_KEY
import com.solthariel.pawchan.boards.boardCategories
import com.solthariel.pawchan.boards.filterBoards
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BoardsViewModelTest {
    @Test
    fun combinesDirectoryFavoritesAndServerSpeeds() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val preferences = FakeBoardPreferences()
                val viewModel =
                    BoardsViewModel(
                        FakeBoardDirectoryRepository(boards()),
                        preferences,
                        FakePostingAccess(),
                        dispatcher,
                    )
                advanceUntilIdle()

                viewModel.setFavorite(boardId("b"), true)
                advanceUntilIdle()

                val content = viewModel.state.value as BoardsUiState.Content
                assertEquals(setOf("b"), content.favoriteBoards.map { it.value }.toSet())
                assertEquals(136, content.boardSpeeds[boardId("b")])
                assertEquals(1, preferences.favoriteWrites)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun categoryExpansionIsKeptInScreenState() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val preferences = FakeBoardPreferences()
                val viewModel =
                    BoardsViewModel(
                        FakeBoardDirectoryRepository(boards()),
                        preferences,
                        FakePostingAccess(),
                        dispatcher,
                    )
                advanceUntilIdle()

                viewModel.setCategoryExpanded("Разное", true)
                viewModel.setCategoryExpanded("Японская культура", true)
                viewModel.setCategoryExpanded("Разное", false)
                advanceUntilIdle()

                val content = viewModel.state.value as BoardsUiState.Content
                assertEquals(
                    setOf(FAVORITES_CATEGORY_KEY, "Японская культура"),
                    content.expandedCategories,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun protectedBoardIsUnavailableUntilPostingAccessExists() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val protectedBoard = board("fg", "Трапы", "Взрослым", requiresPostingAccess = true)
                val access = FakePostingAccess()
                val viewModel =
                    BoardsViewModel(
                        FakeBoardDirectoryRepository(listOf(protectedBoard)),
                        FakeBoardPreferences(),
                        access,
                        dispatcher,
                    )
                advanceUntilIdle()

                var content = viewModel.state.value as BoardsUiState.Content
                assertEquals(setOf(boardId("fg")), content.unavailableBoards)

                access.grant()
                advanceUntilIdle()

                content = viewModel.state.value as BoardsUiState.Content
                assertEquals(emptySet<BoardId>(), content.unavailableBoards)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun categoriesUseStableSiteOrderAndPutUserBoardsLast() {
        val categories = boardCategories(boards())

        assertEquals(
            listOf("Японская культура", "Разное", "Пользовательские"),
            categories.map { it.title },
        )
        assertEquals(listOf("a"), categories.first().boards.map { it.address.board.value })
        assertEquals(listOf("b", "d"), categories[1].boards.map { it.address.board.value })
    }

    @Test
    fun searchMatchesCodeTitleAndCategory() {
        val boards = boards()

        assertEquals(listOf("b"), filterBoards(boards, "/b/").map { it.address.board.value })
        assertEquals(listOf("a"), filterBoards(boards, "аниме").map { it.address.board.value })
        assertEquals(listOf("test"), filterBoards(boards, "пользовательские").map { it.address.board.value })
    }

    @Test fun observedIndexSpeedUpdatesTheBoardListWithoutAnotherDirectoryRequest() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val initial = boards()
                val address = initial.first { it.address.board.value == "b" }.address
                val observed = MutableStateFlow<Map<com.solthariel.pawchan.domain.model.BoardAddress, Int>>(emptyMap())
                var requests = 0
                val response = DomainOutcome.Success(initial)
                val directory =
                    object : BoardDirectoryRepository {
                        override val boardSpeeds = observed

                        override suspend fun loadBoards(forceRefresh: Boolean) = response.also { requests++ }
                    }
                val model = BoardsViewModel(directory, FakeBoardPreferences(), FakePostingAccess(), dispatcher)
                advanceUntilIdle()
                observed.value = mapOf(address to 1529)
                advanceUntilIdle()
                assertEquals(1529, (model.state.value as BoardsUiState.Content).boardSpeeds[address.board])
                observed.value = mapOf(address to 0)
                advanceUntilIdle()
                assertEquals(0, (model.state.value as BoardsUiState.Content).boardSpeeds[address.board])
                assertEquals(1, requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun boards(): List<BoardDirectoryEntry> =
        listOf(
            board("b", "Бред", "Разное", postsPerHour = 136),
            board("d", "Скла/d/ грязи", "Разное"),
            board("test", "Тестовая", "Пользовательские"),
            board("a", "Аниме", "Японская культура"),
        )

    private fun board(
        code: String,
        title: String,
        category: String,
        postsPerHour: Int = 0,
        requiresPostingAccess: Boolean = false,
    ): BoardDirectoryEntry =
        BoardDirectoryEntry(
            address = BoardAddress(siteId(), boardId(code)),
            title = title,
            category = category,
            postsPerHour = postsPerHour,
            postingEnabled = true,
            requiresPostingAccess = requiresPostingAccess,
        )

    private fun boardId(value: String): BoardId = requireNotNull(BoardId.parse(value))

    private fun siteId(): SiteId = requireNotNull(SiteId.parse("dvach"))
}

private typealias BoardDirectoryOutcome = DomainOutcome<List<BoardDirectoryEntry>>

private class FakeBoardDirectoryRepository(
    private val boards: List<BoardDirectoryEntry>,
) : BoardDirectoryRepository {
    override suspend fun loadBoards(forceRefresh: Boolean): BoardDirectoryOutcome = DomainOutcome.Success(boards)
}

private class FakeBoardPreferences : BoardPreferencesRepository {
    private val favorites = MutableStateFlow(emptySet<BoardId>())
    private val expanded = MutableStateFlow(setOf(FAVORITES_CATEGORY_KEY))
    var favoriteWrites = 0
        private set

    override val favoriteBoards: Flow<Set<BoardId>> = favorites
    override val expandedBoardCategories: Flow<Set<String>> = expanded

    override suspend fun setFavoriteBoard(
        board: BoardId,
        favorite: Boolean,
    ): DomainOutcome<Unit> {
        favoriteWrites++
        favorites.value = if (favorite) favorites.value + board else favorites.value - board
        return DomainOutcome.Success(Unit)
    }

    override suspend fun setBoardCategoryExpanded(
        category: String,
        expanded: Boolean,
    ): DomainOutcome<Unit> {
        this.expanded.value =
            if (expanded) {
                this.expanded.value + category
            } else {
                this.expanded.value - category
            }
        return DomainOutcome.Success(Unit)
    }
}

private class FakePostingAccess : PostingAccessRepository {
    private val access = MutableStateFlow(false)
    override val hasPostingAccess: Flow<Boolean> = access

    fun grant() {
        access.value = true
    }
}
