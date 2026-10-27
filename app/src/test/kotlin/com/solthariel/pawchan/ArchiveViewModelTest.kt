package com.solthariel.pawchan

import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.library.ArchiveViewModel
import com.solthariel.pawchan.navigation.boardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveViewModelTest {
    @Test
    fun pagesUseLatestIndexThenOlderPagesInReverseOrder() =
        withModel { model, repos ->
            model.next()
            advanceUntilIdle()
            model.next()
            advanceUntilIdle()
            model.next()
            advanceUntilIdle()
            assertEquals(listOf(null, 1, 0), repos.archiveRequests)
            assertEquals(2, model.state.value.pageIndex)
            model.previous()
            advanceUntilIdle()
            assertEquals(1, model.state.value.pageIndex)
        }

    @Test
    fun refreshFailureKeepsAlreadyLoadedArchive() =
        withModel { model, repos ->
            val previous = model.state.value.snapshot
            repos.archiveResult = DomainOutcome.Failure(DomainFailure.HttpStatus(429))
            model.refresh()
            advanceUntilIdle()
            assertEquals(previous, model.state.value.snapshot)
            assertEquals(DomainFailure.HttpStatus(429), model.state.value.failure)
            assertFalse(model.state.value.loading)
        }

    @Test
    fun emptyPageListHasOneLatestPageAndNoInvalidRequest() =
        withModel { model, repos ->
            repos.archiveResult =
                DomainOutcome.Success(
                    com.solthariel.pawchan.domain.boards
                        .BoardArchiveSnapshot(emptyList(), emptyList()),
                )
            model.refresh()
            advanceUntilIdle()
            val requests = repos.archiveRequests.toList()
            model.next()
            model.previous()
            advanceUntilIdle()
            assertEquals(requests, repos.archiveRequests)
            assertNotNull(model.state.value.snapshot)
        }

    private fun withModel(action: suspend TestScope.(ArchiveViewModel, FakeBoardMenuRepositories) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repos = FakeBoardMenuRepositories()
            val model =
                ArchiveViewModel(
                    requireNotNull(boardAddress("dvach", "biz")),
                    repos,
                    StandardTestDispatcher(testScheduler),
                )
            try {
                advanceUntilIdle()
                action(model, repos)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}
