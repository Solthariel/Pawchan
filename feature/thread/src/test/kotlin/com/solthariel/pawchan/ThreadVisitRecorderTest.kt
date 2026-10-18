package com.solthariel.pawchan

import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.ThreadVisitRecorder
import com.solthariel.pawchan.thread.ThreadWorkspaceEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ThreadVisitRecorderTest {
    private val address = defaultThreadAddress(42)
    private val snapshot = ThreadSnapshot(address, "Тема", ThreadFlags(false, false, false), emptyList())

    @Test
    fun observationPrecedesAddressedVisitWithInjectedTime() =
        runTest {
            val calls = mutableListOf<String>()
            val library = RecordingVisitLibrary(calls)
            val recorder = ThreadVisitRecorder(address, library, VisitWorkspaceEvents(calls)) { 123L }
            recorder.record(DomainOutcome.Success(snapshot))
            assertEquals(listOf("observe", "visit"), calls)
            assertEquals(Triple(address, snapshot.subject, 123L), library.saved)
        }

    @Test
    fun failedLoadDoesNotInventObservationOrVisit() =
        runTest {
            val calls = mutableListOf<String>()
            val library = RecordingVisitLibrary(calls)
            val recorder = ThreadVisitRecorder(address, library, VisitWorkspaceEvents(calls)) { 123L }
            recorder.record(DomainOutcome.Failure(DomainFailure.NetworkUnavailable))
            assertEquals(emptyList<String>(), calls)
            assertNull(library.saved)
        }

    @Test
    fun missingWorkspacePortStillRecordsVisit() =
        runTest {
            val calls = mutableListOf<String>()
            val library = RecordingVisitLibrary(calls)
            val recorder = ThreadVisitRecorder(address, library, null) { 456L }
            recorder.record(DomainOutcome.Success(snapshot))
            assertEquals(listOf("visit"), calls)
            assertEquals(Triple(address, snapshot.subject, 456L), library.saved)
        }

    @Test
    fun cancellationIsNotConvertedToAVisitResult() =
        runTest {
            val calls = mutableListOf<String>()
            val library = RecordingVisitLibrary(calls, cancel = true)
            val recorder = ThreadVisitRecorder(address, library, VisitWorkspaceEvents(calls)) { 123L }
            try {
                recorder.record(DomainOutcome.Success(snapshot))
                fail("Cancellation must reach the caller")
            } catch (error: CancellationException) {
                assertEquals("cancelled visit", error.message)
            }
            assertEquals(listOf("observe", "visit"), calls)
            assertNull(library.saved)
        }
}

private class RecordingVisitLibrary(
    private val calls: MutableList<String>,
    private val cancel: Boolean = false,
) : ThreadLibraryRepository by FakeThreadLibrary() {
    var saved: Triple<ThreadAddress, String?, Long>? = null
        private set

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit> {
        calls += "visit"
        if (cancel) throw CancellationException("cancelled visit")
        saved = Triple(address, subject, visitedAtEpochMillis)
        return DomainOutcome.Success(Unit)
    }
}

private class VisitWorkspaceEvents(
    private val calls: MutableList<String>,
) : ThreadWorkspaceEvents {
    override fun observe(snapshot: ThreadSnapshot) {
        calls += "observe"
    }

    override fun storeOwn(post: BoardPost) {
        calls += "own"
    }
}
