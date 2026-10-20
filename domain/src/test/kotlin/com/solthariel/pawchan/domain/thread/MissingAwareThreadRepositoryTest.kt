package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissingAwareThreadRepositoryTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42L)),
        )
    private val missing = mutableSetOf<ThreadAddress>()
    private var calls = 0
    private var reason: DomainFailure = DomainFailure.NotFound
    private val store =
        object : ThreadDeletionStore {
            override suspend fun contains(address: ThreadAddress): Boolean = address in missing

            override suspend fun markDeleted(address: ThreadAddress) {
                missing.add(address)
            }
        }
    private val remote =
        object : ReportingThreadRepository {
            override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                calls++
                return DomainOutcome.Failure(reason)
            }

            override suspend fun reportPost(
                address: ThreadAddress,
                post: PostId,
                comment: String,
            ): PostingOutcome<Unit> = PostingOutcome.Success(Unit)
        }

    @Test fun confirmedDeletionSurvivesRepositoryRecreationWithoutAnotherRequest() =
        runTest {
            MissingAwareThreadRepository(remote, store).loadThread(address)
            reason = DomainFailure.NetworkUnavailable
            val reopened = MissingAwareThreadRepository(remote, store)
            assertTrue(reopened.isThreadDeleted(address))
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), reopened.loadThread(address))
            assertEquals(1, calls)
        }

    @Test fun networkFailuresAreRetriedAndNeverSavedAsDeleted() =
        runTest {
            reason = DomainFailure.NetworkUnavailable
            val repository = MissingAwareThreadRepository(remote, store)
            repeat(2) { repository.loadThread(address) }
            assertFalse(repository.isThreadDeleted(address))
            assertTrue(missing.isEmpty())
            assertEquals(2, calls)
        }

    @Test fun concurrentChecksOnlyContactTheServerOnceWhenDeletionIsConfirmed() =
        runTest {
            val repository = MissingAwareThreadRepository(remote, store)
            (0 until 8).map { async { repository.loadThread(address) } }.awaitAll()
            assertEquals(1, calls)
        }

    @Test fun anotherBoardWithTheSameThreadNumberIsNotDeleted() =
        runTest {
            val repository = MissingAwareThreadRepository(remote, store)
            repository.loadThread(address)
            val other = address.copy(board = address.board.copy(board = requireNotNull(BoardId.parse("mobi"))))
            assertFalse(repository.isThreadDeleted(other))
        }
}
