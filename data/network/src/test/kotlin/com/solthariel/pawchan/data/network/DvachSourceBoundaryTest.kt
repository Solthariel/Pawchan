package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveSubmission
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.thread.PostVote
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DvachSourceBoundaryTest {
    private val calls = Call.Factory { throw AssertionError("A foreign site must never reach the 2ch network") }
    private val board = BoardAddress(requireNotNull(SiteId.parse("other-forum")), requireNotNull(BoardId.parse("b")))
    private val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))

    @Test fun foreignReadingNeverBorrowsDvachEndpointsEvenWhenIdentifiersCollide() =
        runTest {
            val directory =
                object : BoardDirectoryRepository {
                    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
                        throw AssertionError("Foreign source must not consult the 2ch directory")
                }
            val expected = DomainOutcome.Failure(DomainFailure.UnsupportedSite)
            assertEquals(expected, DvachThreadApi.create(calls).loadThread(thread))
            assertEquals(expected, DvachCatalogApi.create(calls, directory).loadCatalog(board))
            assertEquals(expected, DvachBoardDetailsApi.create(calls).loadArchive(board, null))
            assertEquals(expected, DvachBoardDetailsApi.create(calls).loadInformation(board))
        }

    @Test fun foreignPostingAndCaptchaAreRejectedBeforeCreatingAnyCall() =
        runTest {
            val posting = DvachPostingApi.create(calls)
            val file = ReplyAttachment("a.png", "image/png", byteArrayOf(1))
            val post = PostAddress(thread, requireNotNull(PostId.parse(43)))
            val results =
                listOf(
                    posting.loadRules(board),
                    posting.loadCaptcha(board, thread.thread),
                    posting.submitReply(thread, ReplyDraft("text", null, emptyList())),
                    posting.createThread(board, NewThreadDraft("topic", "text", null, listOf(file))),
                    DvachThreadApi.create(calls).reportPost(thread, requireNotNull(PostId.parse(43)), "report"),
                    DvachThreadApi.create(calls).vote(post, PostVote.LIKE),
                )
            assertTrue(results.all { it is PostingOutcome.Failure && it.reason is PostingFailure.Rejected })
        }

    @Test fun foreignArchiveCannotFabricateADvachSourceUrl() =
        runTest {
            val archive = ArhivachApi(calls)
            assertEquals(
                DomainOutcome.Failure(DomainFailure.UnsupportedSite),
                archive.load(thread, "https://arhivach.vc/thread/42/"),
            )
            assertTrue(archive.add(thread, "answer") is ArchiveSubmission.Rejected)
        }
}
