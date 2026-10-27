package com.solthariel.pawchan

import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.boards.ArchivedThreadSummary
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.library.ArchiveViewModel
import com.solthariel.pawchan.navigation.boardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveRowDispatchTest {
    @Test
    fun rowsArePublishedOnlyAfterBackgroundPreparationCompletes() =
        withPendingRows { model, background ->
            assertTrue(model.state.value.loading)
            assertNull(model.state.value.snapshot)
            assertTrue(
                model.state.value.rows
                    .isEmpty(),
            )
            background.runCurrent()
            runCurrent()
            assertFalse(model.state.value.loading)
            assertEquals(
                "Тема",
                model.state.value.rows
                    .single()
                    .subject,
            )
            assertEquals(
                "№42",
                model.state.value.rows
                    .single()
                    .metadata,
            )
            assertEquals(
                42L,
                model.state.value.snapshot
                    ?.threads
                    ?.single()
                    ?.address
                    ?.thread
                    ?.value,
            )
        }

    @Test
    fun cancellationDoesNotPublishPendingRows() =
        withPendingRows { model, background ->
            model.viewModelScope.cancel()
            background.runCurrent()
            runCurrent()
            assertTrue(
                model.state.value.rows
                    .isEmpty(),
            )
            assertNull(model.state.value.snapshot)
        }

    private fun withPendingRows(action: suspend TestScope.(ArchiveViewModel, TestCoroutineScheduler) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val board = requireNotNull(boardAddress("dvach", "biz"))
            val background = TestCoroutineScheduler()
            val repository = FakeBoardMenuRepositories()
            repository.archiveResult =
                DomainOutcome.Success(
                    BoardArchiveSnapshot(
                        listOf(ArchivedThreadSummary(ThreadAddress(board, requireNotNull(ThreadId.parse(42))), "Тема")),
                        emptyList(),
                    ),
                )
            val model = ArchiveViewModel(board, repository, StandardTestDispatcher(background))
            try {
                runCurrent()
                action(model, background)
            } finally {
                model.viewModelScope.cancel()
                background.runCurrent()
                runCurrent()
                Dispatchers.resetMain()
            }
        }
}
