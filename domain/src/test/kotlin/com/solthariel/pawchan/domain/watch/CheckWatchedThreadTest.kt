package com.solthariel.pawchan.domain.watch

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckWatchedThreadTest {
    @Test
    fun returnsOnlyPostsAfterCheckpoint() =
        runBlocking {
            val subscription = subscription(checkpoint = 43L)
            val useCase = CheckWatchedThread(FakeThreadRepository(snapshot(42L, 43L, 44L)))

            val outcome = useCase(subscription) as DomainOutcome.Success
            val result = outcome.value as ThreadWatchResult.NewPosts

            assertEquals(listOf(44L), result.posts.map { post -> post.address.post.value })
            assertEquals(44L, result.checkpoint.value)
        }

    @Test
    fun convertsNotFoundToDeleted() =
        runBlocking {
            val useCase =
                CheckWatchedThread(
                    FakeThreadRepository(
                        DomainOutcome.Failure(DomainFailure.NotFound),
                    ),
                )

            val outcome = useCase(subscription(checkpoint = 43L)) as DomainOutcome.Success

            assertTrue(outcome.value is ThreadWatchResult.Deleted)
        }

    @Test
    fun reportsClosedTogetherWithFinalPosts() =
        runBlocking {
            val useCase =
                CheckWatchedThread(
                    FakeThreadRepository(snapshot(42L, 43L, 44L, closed = true)),
                )

            val outcome = useCase(subscription(checkpoint = 43L)) as DomainOutcome.Success
            val result = outcome.value as ThreadWatchResult.Closed

            assertEquals(listOf(44L), result.posts.map { post -> post.address.post.value })
        }

    @Test
    fun neverMovesCheckpointBackwards() =
        runBlocking {
            val useCase = CheckWatchedThread(FakeThreadRepository(snapshot(42L)))

            val outcome = useCase(subscription(checkpoint = 43L)) as DomainOutcome.Success
            val result = outcome.value as ThreadWatchResult.Unchanged

            assertEquals(43L, result.checkpoint?.value)
        }

    private fun subscription(checkpoint: Long): ThreadWatchSubscription =
        ThreadWatchSubscription(
            address = address(),
            subject = "Тред",
            lastKnownPostId = requireNotNull(PostId.parse(checkpoint)),
            enabledAtEpochMillis = 1L,
        )

    private fun snapshot(
        vararg postIds: Long,
        closed: Boolean = false,
    ): DomainOutcome<ThreadSnapshot> =
        DomainOutcome.Success(
            ThreadSnapshot(
                address = address(),
                subject = "Тред",
                flags = ThreadFlags(pinned = false, closed = closed, archived = false),
                posts =
                    postIds.map { postId ->
                        BoardPost(
                            address =
                                PostAddress(
                                    address(),
                                    requireNotNull(PostId.parse(postId)),
                                ),
                            content =
                                PostContent(
                                    document = PostDocument.plain("Пост $postId"),
                                    metadata = PostMetadata(name = null, date = null, sage = false),
                                ),
                            attachments = emptyList(),
                        )
                    },
            ),
        )

    private fun address(): ThreadAddress =
        ThreadAddress(
            board =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("b")),
                ),
            thread = requireNotNull(ThreadId.parse(42L)),
        )
}

private class FakeThreadRepository(
    private val outcome: DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> = outcome
}
