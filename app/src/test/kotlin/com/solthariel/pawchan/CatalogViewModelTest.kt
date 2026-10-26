package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogError
import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.catalog.CatalogViewModel
import com.solthariel.pawchan.core.ui.RefreshFailure
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.CatalogThread
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    @Test
    fun contentFiltersRecalculateWithoutNetworkAndRevealRespectsSearch() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val rules =
                    MutableStateFlow(
                        listOf(
                            ContentFilterRule(
                                "rule",
                                "dvach",
                                condition = ContentFilterConditionSpec(values = listOf("OP")),
                            ),
                        ),
                    )
                val repository = FakeCatalogRepository(success())
                val model = CatalogViewModel(repository, uiDispatcher = dispatcher, rules = rules)
                advanceUntilIdle()
                val hidden = model.state.value as CatalogUiState.Content
                assertEquals(0, hidden.threads.size)
                assertEquals(1, hidden.autoHiddenThreads.size)
                assertEquals(true, hidden.allThreadsFiltered)
                model.toggleFilteredThreads()
                advanceUntilIdle()
                assertEquals(1, (model.state.value as CatalogUiState.Content).threads.size)
                model.setSearchQuery("не существует")
                advanceUntilIdle()
                assertEquals(0, (model.state.value as CatalogUiState.Content).threads.size)
                rules.value = emptyList()
                model.setSearchQuery("")
                advanceUntilIdle()
                assertEquals(1, (model.state.value as CatalogUiState.Content).threads.size)
                assertEquals(1, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun loadsCatalogIntoContentState() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = FakeCatalogRepository(success())
                val viewModel = CatalogViewModel(repository)

                advanceUntilIdle()

                val content = viewModel.state.value as CatalogUiState.Content
                assertEquals("Бред", content.boardTitle)
                val thread = content.threads.single()
                assertEquals(42L, thread.id)
                assertEquals("OP", thread.comment)
                assertEquals("Аноним", thread.author)
                assertEquals(1, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun normalizesCatalogPreviewWhitespace() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository =
                    FakeCatalogRepository(
                        success(comment = "Первая строка\n\n\nВторая строка   \n\n"),
                    )
                val viewModel = CatalogViewModel(repository)

                advanceUntilIdle()

                val content = viewModel.state.value as CatalogUiState.Content
                val thread = content.threads.single()
                assertEquals("Первая строка\nВторая строка", thread.comment)
                assertEquals("Первая строка\n\n\nВторая строка   \n\n", thread.content.filterText)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshKeepsContentAndCancelsPreviousRequest() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = RefreshCatalogRepository(success())
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()

                viewModel.refresh()
                runCurrent()
                assertEquals(true, (viewModel.state.value as CatalogUiState.Content).isRefreshing)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(false, (viewModel.state.value as CatalogUiState.Content).isRefreshing)
                assertEquals(1, repository.cancellations)
                assertEquals(3, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedRefreshKeepsReadableCatalog() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeCatalogRepository(success(), DomainOutcome.Failure(DomainFailure.HttpStatus(429)))
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()
                val old = viewModel.state.value as CatalogUiState.Content
                viewModel.refresh()
                advanceUntilIdle()
                val content = viewModel.state.value as CatalogUiState.Content
                assertEquals(old.threads, content.threads)
                assertEquals(false, content.isRefreshing)
                assertEquals(RefreshFailure(rateLimited = true), content.refreshFailure)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun retryReplacesErrorWithFreshContent() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository =
                    FakeCatalogRepository(
                        DomainOutcome.Failure(DomainFailure.NetworkUnavailable),
                        success(),
                    )
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()
                assertEquals(
                    CatalogError.NETWORK,
                    (viewModel.state.value as CatalogUiState.Error).reason,
                )

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals("Бред", (viewModel.state.value as CatalogUiState.Content).boardTitle)
                assertEquals(2, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshKeepsCachedBoardSpeedWhenApiOmitsIt() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository =
                    FakeCatalogRepository(
                        success(boardSpeed = 136),
                        success(boardSpeed = 0, boardSpeedAvailable = false),
                    )
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()
                assertEquals(136, (viewModel.state.value as CatalogUiState.Content).boardSpeed)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(136, (viewModel.state.value as CatalogUiState.Content).boardSpeed)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshAcceptsReportedZeroInsteadOfKeepingCachedSpeed() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = FakeCatalogRepository(success(boardSpeed = 136), success(boardSpeed = 0))
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()
                assertEquals(136, (viewModel.state.value as CatalogUiState.Content).boardSpeed)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(0, (viewModel.state.value as CatalogUiState.Content).boardSpeed)
                assertEquals(2, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshReplacesCachedSpeedWithLatestReportedValue() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = FakeCatalogRepository(success(boardSpeed = 136), success(boardSpeed = 148))
                val viewModel = CatalogViewModel(repository)
                advanceUntilIdle()
                assertEquals(136, (viewModel.state.value as CatalogUiState.Content).boardSpeed)

                viewModel.refresh()
                advanceUntilIdle()

                assertEquals(148, (viewModel.state.value as CatalogUiState.Content).boardSpeed)
                assertEquals(2, repository.requests)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun loadsCatalogForFullRequestedAddress() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = FakeCatalogRepository(success())
                val address =
                    BoardAddress(
                        site = requireNotNull(SiteId.parse("dvach")),
                        board = requireNotNull(BoardId.parse("po")),
                    )
                CatalogViewModel(repository, address)
                advanceUntilIdle()

                assertEquals(listOf(address), repository.boards)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun searchDebouncesAndCancelsPreviousQuery() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val viewModel = CatalogViewModel(FakeCatalogRepository(success()))
                advanceUntilIdle()

                viewModel.setSearchQuery("нет совпадения")
                advanceTimeBy(119L)
                assertEquals(1, (viewModel.state.value as CatalogUiState.Content).threads.size)

                viewModel.setSearchQuery("OP")
                advanceTimeBy(120L)
                runCurrent()
                assertEquals(1, (viewModel.state.value as CatalogUiState.Content).threads.size)

                viewModel.setSearchQuery("нет совпадения")
                advanceTimeBy(120L)
                runCurrent()
                assertEquals(0, (viewModel.state.value as CatalogUiState.Content).threads.size)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun success(
        subject: String = "Тестовый тред",
        comment: String = "OP",
        boardSpeed: Int = 0,
        boardSpeedAvailable: Boolean = true,
    ): DomainOutcome<CatalogSnapshot> {
        val board =
            BoardAddress(
                site = requireNotNull(SiteId.parse("dvach")),
                board = requireNotNull(BoardId.parse("b")),
            )
        val thread =
            CatalogThread(
                address = ThreadAddress(board, requireNotNull(ThreadId.parse(42L))),
                subject = subject,
                content =
                    PostContent(
                        document = PostDocument.plain(comment),
                        metadata = PostMetadata(name = "Аноним", date = null, sage = false),
                    ),
                attachments = emptyList(),
                statistics = requireNotNull(ThreadStatistics.create(3, 0, 0)),
                flags = ThreadFlags(pinned = false, closed = false, archived = false),
            )
        return DomainOutcome.Success(
            CatalogSnapshot(
                board = BoardDescriptor(board, "Бред", worksafe = false),
                threads = listOf(thread),
                boardSpeed = boardSpeed,
                boardSpeedAvailable = boardSpeedAvailable,
            ),
        )
    }
}

private class FakeCatalogRepository(
    vararg outcomes: DomainOutcome<CatalogSnapshot>,
) : CatalogRepository {
    private val outcomes = ArrayDeque(outcomes.toList())

    var requests: Int = 0
        private set
    val boards = mutableListOf<BoardAddress>()

    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> {
        requests += 1
        boards += board
        return outcomes.removeFirst()
    }
}

private class RefreshCatalogRepository(
    private val outcome: DomainOutcome<CatalogSnapshot>,
) : CatalogRepository {
    var requests: Int = 0
        private set
    var cancellations: Int = 0
        private set

    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> {
        requests += 1
        if (requests == BLOCKING_REQUEST) {
            try {
                awaitCancellation()
            } finally {
                cancellations += 1
            }
        }
        return outcome
    }
}

private const val BLOCKING_REQUEST = 2
