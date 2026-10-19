package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.infrastructure.UsagePostingRepository
import com.solthariel.pawchan.navigation.boardAddress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsagePostingRepositoryTest {
    private val board = requireNotNull(boardAddress("dvach", "b"))
    private val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))
    private val files =
        listOf(
            ReplyAttachment("picture.jpg", "image/jpeg", byteArrayOf(1)),
            ReplyAttachment("video.webm", "video/webm", byteArrayOf(2)),
            ReplyAttachment("other.bin", null, byteArrayOf(3)),
        )

    @Test fun confirmedReplyCountsPreparedAttachmentsAndPreservesResult() =
        runBlocking {
            val stats = RecordingUsage()
            val delegate = FakePostingRepository()
            val posting = UsagePostingRepository(delegate, stats)
            val result = posting.submitReply(address, ReplyDraft("text", null, files))
            assertTrue(result is PostingOutcome.Success)
            assertEquals(files, delegate.lastDraft?.attachments)
            listOf(
                UsageMetric.POSTS_SENT,
                UsageMetric.REPLIES_SENT,
                UsageMetric.IMAGES_SENT,
                UsageMetric.VIDEOS_SENT,
            ).forEach { metric ->
                assertEquals(1L, stats.events.filter { it.first == metric }.sumOf { it.second })
            }
            assertEquals(4, stats.events.size)
            assertTrue(stats.events.none { it.first == UsageMetric.THREADS_CREATED })
        }

    @Test fun samePreparedFilesKeepIdentityAcrossDifferentPostsAndAttachmentOrder() =
        runBlocking {
            val stats = RecordingUsage()
            val posting = UsagePostingRepository(FakePostingRepository(), stats)
            posting.submitReply(address, ReplyDraft("first", null, files))
            posting.createThread(board, NewThreadDraft("title", "second", null, files.reversed()))
            listOf(UsageMetric.IMAGES_SENT, UsageMetric.VIDEOS_SENT).forEach { metric ->
                val ids = stats.events.filter { it.first == metric }.map { it.third }
                assertEquals(2, ids.size)
                assertEquals(ids.first(), ids.last())
                assertTrue(ids.first()?.startsWith("content:") == true)
            }
        }

    @Test fun failureDoesNotCountPostOrFiles() =
        runBlocking {
            val stats = RecordingUsage()
            val delegate =
                object : PostingRepository by FakePostingRepository() {
                    override suspend fun submitReply(
                        address: ThreadAddress,
                        draft: ReplyDraft,
                    ): PostingOutcome<PostId> = PostingOutcome.Failure(PostingFailure.NetworkUnavailable)
                }
            val outcome = UsagePostingRepository(delegate, stats).submitReply(address, ReplyDraft("text", null, files))
            assertTrue(outcome is PostingOutcome.Failure)
            assertTrue(stats.events.isEmpty())
        }

    @Test fun newThreadUsesCreatedCounterInsteadOfReplyCounter() =
        runBlocking {
            val stats = RecordingUsage()
            val posting = UsagePostingRepository(FakePostingRepository(), stats)
            posting.createThread(board, NewThreadDraft("title", "text", null, files))
            assertEquals(1, stats.events.count { it.first == UsageMetric.THREADS_CREATED })
            assertTrue(stats.events.none { it.first == UsageMetric.REPLIES_SENT })
        }

    private class RecordingUsage : UsageStatisticsRepository {
        override val days = MutableStateFlow(emptyList<UsageDay>())
        override val available = MutableStateFlow(true)
        val events = mutableListOf<Triple<UsageMetric, Long, String?>>()

        override suspend fun record(
            metric: UsageMetric,
            amount: Long,
            identity: String?,
        ) {
            events.add(Triple(metric, amount, identity))
        }

        override suspend fun reset() = events.clear()
    }
}
