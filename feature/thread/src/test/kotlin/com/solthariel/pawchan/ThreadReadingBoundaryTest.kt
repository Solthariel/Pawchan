package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.thread.ThreadReadingSession
import com.solthariel.pawchan.thread.collectPositions
import com.solthariel.pawchan.thread.threadListAnchor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadReadingBoundaryTest {
    @Test
    fun visiblePositionIsImmediateButStorageWaitsForDebounce() =
        runTest {
            val positions = MutableSharedFlow<ThreadReadingPosition>()
            val session = RecordingReadingSession()
            val address = defaultThreadAddress(42)
            val job = backgroundScope.launch { session.collectPositions(address, positions) }
            runCurrent()
            positions.emit(ThreadReadingPosition(7, 12))
            runCurrent()
            assertEquals(listOf(address), session.ready)
            assertEquals(listOf(address to ThreadReadingPosition(7, 12)), session.visible)
            assertTrue(session.persisted.isEmpty())
            advanceTimeBy(299)
            runCurrent()
            assertTrue(session.persisted.isEmpty())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(session.visible, session.persisted)
            job.cancel()
        }

    @Test
    fun latestOffsetWinsAndEqualPositionsDoNotRestartTheTimer() =
        runTest {
            val positions = MutableSharedFlow<ThreadReadingPosition>()
            val session = RecordingReadingSession()
            val address = defaultThreadAddress(42)
            backgroundScope.launch { session.collectPositions(address, positions) }
            runCurrent()
            positions.emit(ThreadReadingPosition(7, 12))
            runCurrent()
            advanceTimeBy(200)
            positions.emit(ThreadReadingPosition(8, 23))
            runCurrent()
            advanceTimeBy(200)
            positions.emit(ThreadReadingPosition(8, 23))
            runCurrent()
            advanceTimeBy(100)
            runCurrent()
            assertEquals(2, session.visible.size)
            assertEquals(listOf(address to ThreadReadingPosition(8, 23)), session.persisted)
        }

    @Test
    fun leavingOwnerCancelsPendingSaveAndOldOwnerReceivesNoNewPositions() =
        runTest {
            val positions = MutableSharedFlow<ThreadReadingPosition>()
            val session = RecordingReadingSession()
            val job = backgroundScope.launch { session.collectPositions(defaultThreadAddress(42), positions) }
            runCurrent()
            positions.emit(ThreadReadingPosition(7, 12))
            runCurrent()
            advanceTimeBy(100)
            job.cancel()
            runCurrent()
            positions.emit(ThreadReadingPosition(8, 0))
            advanceTimeBy(500)
            runCurrent()
            assertEquals(1, session.visible.size)
            assertTrue(session.persisted.isEmpty())
        }

    @Test
    fun restorationUsesStablePostIdAndKeepsTheLegacyFallbackOffset() {
        val positions = mapOf(8L to 3, 7L to 2)
        assertEquals(2 to 12, threadListAnchor(positions, ThreadReadingPosition(7, 12)))
        assertEquals(0 to 12, threadListAnchor(positions, ThreadReadingPosition(9, 12)))
        assertEquals(0 to 0, threadListAnchor(positions, null))
        assertEquals(0 to 12, threadListAnchor(null, ThreadReadingPosition(7, 12)))
    }
}

private class RecordingReadingSession : ThreadReadingSession {
    val ready = mutableListOf<ThreadAddress>()
    val visible = mutableListOf<Pair<ThreadAddress, ThreadReadingPosition>>()
    val persisted = mutableListOf<Pair<ThreadAddress, ThreadReadingPosition>>()

    override fun restore(address: ThreadAddress): ThreadReadingPosition? = null

    override fun contentReady(address: ThreadAddress) {
        ready += address
    }

    override fun visible(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) {
        visible += address to position
    }

    override fun persist(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) {
        persisted += address to position
    }
}
