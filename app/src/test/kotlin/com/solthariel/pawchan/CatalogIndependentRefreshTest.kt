package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.catalog.CatalogViewModel
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardDescriptor
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogIndependentRefreshTest {
    @Test fun slowSpeedNeverBlocksTheListAndZeroIsAcceptedLaterWithoutResettingScroll() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val speed = CompletableDeferred<Int?>()
                val repository =
                    object : CatalogRepository {
                        override suspend fun loadCatalog(board: BoardAddress) = snapshot(board)

                        override suspend fun loadBoardSpeed(board: BoardAddress): Int? = speed.await()
                    }
                val model = CatalogViewModel(repository, uiDispatcher = dispatcher)
                runCurrent()
                assertEquals(false, (model.state.value as CatalogUiState.Content).isRefreshing)
                model.refresh()
                runCurrent()
                val revision = (model.state.value as CatalogUiState.Content).refreshRevision
                assertEquals(1L, revision)
                speed.complete(0)
                advanceUntilIdle()
                val content = model.state.value as CatalogUiState.Content
                assertEquals(0, content.boardSpeed)
                assertEquals(revision, content.refreshRevision)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun failedRefreshKeepsItsPositionAndSuccessfulManualRefreshRequestsTop() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                var calls = 0
                val repository =
                    CatalogRepository { board ->
                        calls++
                        if (calls == 2) DomainOutcome.Failure(DomainFailure.HttpStatus(500)) else snapshot(board)
                    }
                val model = CatalogViewModel(repository, uiDispatcher = dispatcher)
                advanceUntilIdle()
                assertEquals(0L, (model.state.value as CatalogUiState.Content).refreshRevision)
                model.refresh()
                advanceUntilIdle()
                assertEquals(0L, (model.state.value as CatalogUiState.Content).refreshRevision)
                model.refresh()
                advanceUntilIdle()
                assertEquals(1L, (model.state.value as CatalogUiState.Content).refreshRevision)
                model.refreshThreadAges()
                advanceUntilIdle()
                assertEquals(1L, (model.state.value as CatalogUiState.Content).refreshRevision)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun snapshot(board: BoardAddress = defaultBoardAddress()): DomainOutcome<CatalogSnapshot> =
        DomainOutcome.Success(CatalogSnapshot(BoardDescriptor(board, "Бред", false), emptyList(), 1427, true))
}
