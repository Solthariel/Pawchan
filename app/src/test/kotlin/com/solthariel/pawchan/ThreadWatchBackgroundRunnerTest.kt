package com.solthariel.pawchan

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
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.watch.BackgroundWatchRunResult
import com.solthariel.pawchan.watch.ThreadWatchBackgroundRunner
import com.solthariel.pawchan.watch.ThreadWatchEventSink
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadWatchBackgroundRunnerTest {
    @Test
    fun advancesCheckpointAfterSuccessfulCheck() =
        runTest {
            val watches = FakeBackgroundWatches(subscription(43L))
            val events = mutableListOf<Int>()
            val runner =
                ThreadWatchBackgroundRunner(
                    watches,
                    CheckWatchedThread(FakeBackgroundThreads(snapshot(43L, 44L))),
                    ThreadWatchEventSink { _, count -> events += count },
                )

            val result = runner.run()

            assertEquals(BackgroundWatchRunResult.SUCCESS, result)
            assertEquals(listOf(1), events)
            assertEquals(
                44L,
                watches.subscriptions
                    .single()
                    .lastKnownPostId
                    ?.value,
            )
        }

    @Test
    fun removesClosedSubscription() =
        runTest {
            val watches = FakeBackgroundWatches(subscription(43L))
            val runner =
                ThreadWatchBackgroundRunner(
                    watches,
                    CheckWatchedThread(FakeBackgroundThreads(snapshot(43L, closed = true))),
                )

            val result = runner.run()

            assertEquals(BackgroundWatchRunResult.SUCCESS, result)
            assertEquals(emptyList<Any>(), watches.subscriptions)
        }

    @Test
    fun transientFailureRequestsRetry() =
        runTest {
            val watches = FakeBackgroundWatches(subscription(43L))
            val runner =
                ThreadWatchBackgroundRunner(
                    watches,
                    CheckWatchedThread(
                        FakeBackgroundThreads(
                            DomainOutcome.Failure(DomainFailure.NetworkUnavailable),
                        ),
                    ),
                )

            assertEquals(BackgroundWatchRunResult.RETRY, runner.run())
            assertEquals(1, watches.subscriptions.size)
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
                            address = PostAddress(address(), requireNotNull(PostId.parse(postId))),
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
