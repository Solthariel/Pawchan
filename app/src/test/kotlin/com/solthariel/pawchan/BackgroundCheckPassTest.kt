package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadCheckPassRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.watch.CheckWatchedThread
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.domain.workspace.StoredPost
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.watch.BackgroundWatchRunResult
import com.solthariel.pawchan.watch.ThreadWatchBackgroundRunner
import com.solthariel.pawchan.watch.ThreadWatchEventSink
import com.solthariel.pawchan.workspace.checkWorkspaceReplies
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundCheckPassTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val own = post(42)
    private val reply = post(43)
    private val snapshot = ThreadSnapshot(address, null, ThreadFlags(false, false, false), listOf(own, reply))
    private val subscription = ThreadWatchSubscription(address, null, requireNotNull(PostId.parse(42)), 0)

    @Test
    fun bothConsumersShareOneSnapshotAndNotificationPrecedesCheckpoint() =
        runTest {
            var requests = 0
            val pass =
                ThreadCheckPassRepository(
                    object : ThreadRepository {
                        override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                            requests++
                            return DomainOutcome.Success(snapshot)
                        }
                    },
                )
            val workspace = PassWorkspace(WorkspaceState(posts = listOf(StoredPost(own, null, own = true))))
            val order = mutableListOf<String>()
            val base = FakeBackgroundWatches(subscription)
            val watches =
                object : ThreadWatchRepository by base {
                    override suspend fun updateCheckpoint(
                        address: ThreadAddress,
                        lastKnownPostId: PostId,
                    ): DomainOutcome<Unit> {
                        order += "checkpoint"
                        return base.updateCheckpoint(address, lastKnownPostId)
                    }
                }
            val runner =
                ThreadWatchBackgroundRunner(
                    watches,
                    CheckWatchedThread(pass),
                    ThreadWatchEventSink { _, _ -> order += "event" },
                )
            assertTrue(checkWorkspaceReplies(workspace, pass))
            assertEquals(BackgroundWatchRunResult.SUCCESS, runner.run())
            assertEquals(1, requests)
            assertEquals(
                43L,
                workspace.state.replies
                    .single()
                    .post.address.post.value,
            )
            assertEquals(listOf("event", "checkpoint"), order)
        }

    @Test
    fun sharedTemporaryFailureStillRequestsRetryWithoutAdvancingCheckpoint() =
        runTest {
            var requests = 0
            val pass =
                ThreadCheckPassRepository(
                    object : ThreadRepository {
                        override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                            requests++
                            return DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
                        }
                    },
                )
            val workspace = PassWorkspace(WorkspaceState(posts = listOf(StoredPost(own, null, own = true))))
            val watches = FakeBackgroundWatches(subscription)
            assertEquals(false, checkWorkspaceReplies(workspace, pass))
            val result = ThreadWatchBackgroundRunner(watches, CheckWatchedThread(pass)).run()
            assertEquals(BackgroundWatchRunResult.RETRY, result)
            assertEquals(1, requests)
            assertEquals(
                42L,
                watches.subscriptions
                    .single()
                    .lastKnownPostId
                    ?.value,
            )
        }

    private fun post(id: Long): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(
                PostDocument(listOf(PostTextRun("reply", link = PostLink.Reference(requireNotNull(PostId.parse(42)))))),
                PostMetadata(null, null, false),
            ),
            emptyList(),
        )
}

private class PassWorkspace(
    var state: WorkspaceState,
) : WorkspaceRepository {
    override suspend fun load(): WorkspaceState = state

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState {
        state = transform(state)
        return state
    }
}
