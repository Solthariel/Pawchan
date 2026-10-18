package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadSearchFlag
import com.solthariel.pawchan.thread.ThreadToolsState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadToolsStateTest {
    @Test fun typingIsDebouncedAndOnlyTheLatestQueryIsPublished() =
        runTest {
            val tools = ThreadToolsState(this, StandardTestDispatcher(testScheduler))
            val content = toolsContent(listOf(toolsPost(1, "Первый"), toolsPost(2, "Второй")))
            tools.search(content, "Первый")
            runCurrent()
            advanceTimeBy(119)
            assertTrue(tools.results.isEmpty())
            tools.search(content, "Второй")
            advanceUntilIdle()
            assertEquals(listOf(2L), tools.results)
            assertFalse(tools.searching)
        }

    @Test fun closingSearchCancelsPendingResultsAndClearsTheFlag() =
        runTest {
            val tools = ThreadToolsState(this, StandardTestDispatcher(testScheduler))
            tools.setFlag(ThreadSearchFlag.MY, toolsContent(listOf(toolsPost(1))))
            runCurrent()
            tools.closeSearch()
            advanceUntilIdle()
            assertTrue(tools.results.isEmpty())
            assertFalse(tools.searching)
            assertFalse(tools.searchOpen)
            assertEquals(null, tools.flag)
        }

    @Test fun noContentCancelsThePendingSearchWithoutAStuckIndicator() =
        runTest {
            val tools = ThreadToolsState(this, StandardTestDispatcher(testScheduler))
            tools.search(toolsContent(listOf(toolsPost(1))), "Текст")
            runCurrent()
            tools.search(null)
            advanceUntilIdle()
            assertTrue(tools.results.isEmpty())
            assertFalse(tools.searching)
        }

    @Test fun selectionCanBeToggledAndThenCompletelyCleared() =
        runTest {
            val tools = ThreadToolsState(this, StandardTestDispatcher(testScheduler))
            tools.toggleSelection(1)
            tools.toggleSelection(2)
            tools.toggleSelection(1)
            assertEquals(setOf(2L), tools.selected)
            assertTrue(tools.selecting)
            tools.clearSelection()
            assertTrue(tools.selected.isEmpty())
            assertFalse(tools.selecting)
        }
}
