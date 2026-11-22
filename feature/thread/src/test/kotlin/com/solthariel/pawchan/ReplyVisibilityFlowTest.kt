package com.solthariel.pawchan

import com.solthariel.pawchan.thread.collectVisibleReplies
import com.solthariel.pawchan.thread.replyIsVisible
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyVisibilityFlowTest {
    @Test fun needsEnoughVisibleContentAndExcludesCoveredOrEmptyItems() {
        assertTrue(replyIsVisible(10, 100, 0, 200, 48))
        assertTrue(replyIsVisible(160, 100, 0, 200, 40))
        assertFalse(replyIsVisible(160, 100, 0, 200, 48))
        assertFalse(replyIsVisible(0, 100, 0, 0, 48))
        assertFalse(replyIsVisible(0, 0, 0, 200, 48))
        assertTrue(replyIsVisible(10, 20, 0, 200, 48))
        assertFalse(replyIsVisible(190, 20, 0, 200, 48))
    }

    @Test fun onlyVisibleUnreadRepliesAreConsumedAfterStableExposure() =
        runTest {
            val visible = MutableStateFlow(setOf(10L, 11L))
            val unread = MutableStateFlow(setOf(11L, 12L))
            val read = mutableListOf<Set<Long>>()
            backgroundScope.launch { collectVisibleReplies(visible, unread) { read += it } }
            runCurrent()
            advanceTimeBy(499)
            runCurrent()
            assertTrue(read.isEmpty())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf(setOf(11L)), read)
            visible.value = setOf(12L)
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertEquals(listOf(setOf(11L), setOf(12L)), read)
        }

    @Test fun briefExposureOrLeavingTheScreenDoesNotMarkRead() =
        runTest {
            val visible = MutableStateFlow(setOf(11L))
            val unread = MutableStateFlow(setOf(11L))
            val read = mutableListOf<Set<Long>>()
            val job = backgroundScope.launch { collectVisibleReplies(visible, unread) { read += it } }
            runCurrent()
            advanceTimeBy(200)
            visible.value = emptySet()
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertTrue(read.isEmpty())
            visible.value = setOf(11L)
            runCurrent()
            advanceTimeBy(200)
            job.cancel()
            advanceTimeBy(500)
            runCurrent()
            assertTrue(read.isEmpty())
        }

    @Test fun aReplyDiscoveredWhileVisibleIsStillMarkedRead() =
        runTest {
            val visible = MutableStateFlow(setOf(11L))
            val unread = MutableStateFlow(emptySet<Long>())
            val read = mutableListOf<Set<Long>>()
            backgroundScope.launch { collectVisibleReplies(visible, unread) { read += it } }
            runCurrent()
            unread.value = setOf(11L, 12L)
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertEquals(listOf(setOf(11L)), read)
        }
}
