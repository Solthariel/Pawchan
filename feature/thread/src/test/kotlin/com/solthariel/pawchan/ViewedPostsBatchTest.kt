package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.thread.ThreadReadingSession
import com.solthariel.pawchan.thread.collectViewedPosts
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
class ViewedPostsBatchTest {
    @Test fun rapidScrollBatchesVisiblePostsAndFlushesOnCancellation() =
        runTest {
            val events = MutableSharedFlow<List<Long>>()
            val reading = ViewedRecording()
            val job = launch { events.collectViewedPosts(defaultThreadAddress(42), reading) }
            runCurrent()
            events.emit(listOf(1, 2))
            advanceTimeBy(100)
            events.emit(listOf(2, 3))
            advanceTimeBy(100)
            assertTrue(reading.batches.isEmpty())
            advanceTimeBy(301)
            runCurrent()
            assertEquals(listOf(listOf(1L, 2L, 3L)), reading.batches)
            events.emit(listOf(4))
            job.cancel()
            job.join()
            assertEquals(listOf(listOf(1L, 2L, 3L), listOf(4L)), reading.batches)
        }
}

private class ViewedRecording : ThreadReadingSession {
    val batches = mutableListOf<List<Long>>()

    override fun viewed(
        address: ThreadAddress,
        postIds: List<Long>,
    ) {
        batches += postIds
    }

    override fun restore(address: ThreadAddress): ThreadReadingPosition? = null

    override fun contentReady(address: ThreadAddress) = Unit

    override fun visible(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) = Unit

    override fun persist(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) = Unit
}
