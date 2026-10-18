package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.thread.ReportingThreadRepository
import com.solthariel.pawchan.thread.ThreadReportController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadReportControllerTest {
    @Test fun eachSelectedPostIsReportedOnce() =
        runTest {
            val target = ReportTestRepository()
            val controller = ThreadReportController(this, reportTestAddress(), target)
            controller.submit(listOf(2, 3, 2), "Причина")
            advanceUntilIdle()
            assertEquals(listOf(2L, 3L), target.sent)
            assertEquals(2, controller.state.value.sent)
            assertFalse(controller.state.value.sending)
        }

    @Test fun rejectionKeepsItsReasonWithoutUsingComposerState() =
        runTest {
            val controller =
                ThreadReportController(
                    this,
                    reportTestAddress(),
                    ReportFailureRepository(PostingFailure.Rejected("Причина")),
                )
            controller.submit(listOf(2), "Жалоба")
            advanceUntilIdle()
            assertEquals("Причина", controller.state.value.failure)
            assertFalse(controller.state.value.sending)
        }

    @Test fun transportFailureKeepsTheExistingFallbackMessage() =
        runTest {
            val controller =
                ThreadReportController(
                    this,
                    reportTestAddress(),
                    ReportFailureRepository(PostingFailure.NetworkTimeout),
                )
            controller.submit(listOf(2), "Жалоба")
            advanceUntilIdle()
            assertEquals("Не удалось отправить жалобу", controller.state.value.failure)
            assertFalse(controller.state.value.sending)
        }

    @Test fun cancellingStopsTheInFlightOperation() =
        runTest {
            val target = ReportTestRepository(block = CompletableDeferred())
            val controller = ThreadReportController(this, reportTestAddress(), target)
            controller.submit(listOf(2, 3), "Причина")
            runCurrent()
            assertTrue(controller.state.value.sending)
            controller.cancel()
            advanceUntilIdle()
            assertFalse(controller.state.value.sending)
            assertEquals(emptyList<Long>(), target.sent)
        }
}

private class ReportTestRepository(
    private val block: CompletableDeferred<Unit>? = null,
) : ReportingThreadRepository {
    val sent = mutableListOf<Long>()

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        DomainOutcome.Failure(DomainFailure.NetworkUnavailable)

    override suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): PostingOutcome<Unit> {
        block?.await()
        sent += post.value
        return PostingOutcome.Success(Unit)
    }
}

private fun reportTestAddress(): ThreadAddress =
    ThreadAddress(
        BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
        requireNotNull(ThreadId.parse(1)),
    )

private class ReportFailureRepository(
    private val failure: PostingFailure,
) : ReportingThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        DomainOutcome.Failure(DomainFailure.NetworkUnavailable)

    override suspend fun reportPost(
        address: ThreadAddress,
        post: PostId,
        comment: String,
    ): PostingOutcome<Unit> = PostingOutcome.Failure(failure)
}
