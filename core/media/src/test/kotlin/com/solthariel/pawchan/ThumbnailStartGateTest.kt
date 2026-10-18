package com.solthariel.pawchan

import com.solthariel.pawchan.media.thumbnail.ThumbnailStartGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailStartGateTest {
    @Test
    fun taskWaitsExactlyOneDwellWindow() =
        runTest {
            val gate = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(499)
            assertFalse(gate.ready.value)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(gate.ready.value)
            gate.close()
        }

    @Test
    fun lateQueueAdmissionDoesNotAddAnotherDwell() =
        runTest {
            val gate = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(2_000)
            runCurrent()
            assertTrue(gate.ready.value)
            gate.close()
        }

    @Test
    fun startingAnotherTaskDoesNotRestartTheFirstTask() =
        runTest {
            val first = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(250)
            val second = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(250)
            runCurrent()
            assertTrue(first.ready.value)
            assertFalse(second.ready.value)
            advanceTimeBy(250)
            runCurrent()
            assertTrue(second.ready.value)
            first.close()
            second.close()
        }

    @Test
    fun disposalBeforeDwellPreventsAdmission() =
        runTest {
            val gate = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(250)
            gate.close()
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(gate.ready.value)
        }

    @Test
    fun disposalAfterDwellClearsAdmission() =
        runTest {
            val gate = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertTrue(gate.ready.value)
            gate.close()
            assertFalse(gate.ready.value)
        }

    @Test
    fun closingOneTaskDoesNotStopAnotherTask() =
        runTest {
            val first = ThumbnailStartGate(backgroundScope)
            val second = ThumbnailStartGate(backgroundScope)
            runCurrent()
            first.close()
            advanceTimeBy(500)
            runCurrent()
            assertFalse(first.ready.value)
            assertTrue(second.ready.value)
            second.close()
        }

    @Test
    fun cancellingTheOwnerStopsPendingAdmission() =
        runTest {
            val owner =
                CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]))
            val gate = ThumbnailStartGate(owner)
            runCurrent()
            advanceTimeBy(250)
            owner.cancel()
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(gate.ready.value)
            gate.close()
        }

    @Test
    fun admittedTaskRemainsReadyWithoutAnyStopOrReleaseEvent() =
        runTest {
            val gate = ThumbnailStartGate(backgroundScope)
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertTrue(gate.ready.value)
            advanceTimeBy(10_000)
            runCurrent()
            assertTrue(gate.ready.value)
            gate.close()
        }
}
