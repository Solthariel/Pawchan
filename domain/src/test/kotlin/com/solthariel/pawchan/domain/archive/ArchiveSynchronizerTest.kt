package com.solthariel.pawchan.domain.archive

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
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArchiveSynchronizerTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    private fun snapshot(vararg ids: Long): ThreadSnapshot =
        ThreadSnapshot(
            address,
            "subject",
            ThreadFlags(false, false, false),
            ids.map {
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(it))),
                    PostContent(PostDocument(listOf(PostTextRun("text$it"))), PostMetadata(null, null, false)),
                    emptyList(),
                )
            },
        )

    @Test fun mergePreservesDeletedPostsAndRefreshesExisting() {
        val merged = mergeArchivedThread(snapshot(100, 101), snapshot(100, 102))
        assertEquals(listOf(100L, 101L, 102L), merged.posts.map { it.address.post.value })
    }

    @Test fun deadCopiesAreNotPolledAndNothingIsPublishedByBackground() =
        runTest {
            val dead = ThreadCopy(snapshot(100), 1, ArchiveState(phase = ArchivePhase.SOURCE_UNAVAILABLE))
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(dead)))
            val remote = FakeLive(DomainOutcome.Success(snapshot(100, 101)))
            val external = FakeExternal()
            ArchiveSynchronizer(store, remote, external) { 20_000 }.run()
            assertEquals(0, remote.calls)
            assertEquals(0, external.adds)
        }

    @Test fun successfulUpdateKeepsPositionAndDeletedPosts() =
        runTest {
            val copy =
                ThreadCopy(
                    snapshot(100, 101),
                    1,
                    ArchiveState(),
                    OpenThread(address, "title", 101, 32),
                )
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(copy)))
            val remote = FakeLive(DomainOutcome.Success(snapshot(100, 102)))
            ArchiveSynchronizer(store, remote, FakeExternal()) { 20_000 }.run()
            val updated = store.value.copies.single()
            assertEquals(listOf(100L, 101L, 102L), updated.snapshot.posts.map { it.address.post.value })
            assertEquals(copy.position, updated.position)
            assertEquals(ArchivePhase.LOCAL, updated.archive.phase)
        }

    @Test fun failureDoesNotClearCopyOrPretendLastPostWasCaptured() =
        runTest {
            val copy = ThreadCopy(snapshot(100, 101), 1, ArchiveState())
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(copy)))
            assertFalse(
                ArchiveSynchronizer(
                    store,
                    FakeLive(DomainOutcome.Failure(DomainFailure.NotFound)),
                    FakeExternal(),
                ) { 20_000 }.run(),
            )
            val updated = store.value.copies.single()
            assertSame(copy.snapshot, updated.snapshot)
            assertEquals(ArchivePhase.SOURCE_UNAVAILABLE, updated.archive.phase)
        }

    @Test fun fallbackLoadsBoundArchiveWithoutSubmitting() =
        runTest {
            val copy =
                ThreadCopy(
                    snapshot(100),
                    1,
                    ArchiveState(sourceUrl = "https://arhivach.vc/thread/1/"),
                )
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(copy)))
            val external = FakeExternal(DomainOutcome.Success(snapshot(100, 101)))
            assertTrue(
                ArchiveSynchronizer(store, FakeLive(DomainOutcome.Failure(DomainFailure.NotFound)), external) {
                    20_000
                }.run(),
            )
            assertEquals(1, external.loads)
            assertEquals(0, external.adds)
        }

    @Test fun cancellationLeavesCopyIntact() =
        runTest {
            val copy = ThreadCopy(snapshot(100), 1, ArchiveState())
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(copy)))
            val remote =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                        require(address == copy.snapshot.address)
                        throw CancellationException()
                    }
                }
            try {
                ArchiveSynchronizer(store, remote, FakeExternal()) { 20_000 }.run()
                fail("Cancellation expected")
            } catch (
                _: CancellationException,
            ) {
                assertSame(copy, store.value.copies.single())
            }
        }

    @Test fun deletionDuringRequestDoesNotResurrectCopy() =
        runTest {
            val copy = ThreadCopy(snapshot(100), 1, ArchiveState())
            val store = MemoryWorkspace(WorkspaceState(copies = listOf(copy)))
            val remote =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                        store.value = WorkspaceState()
                        return DomainOutcome.Success(snapshot(100, 101))
                    }
                }
            ArchiveSynchronizer(store, remote, FakeExternal()) { 20_000 }.run()
            assertTrue(store.value.copies.isEmpty())
        }
}

private class MemoryWorkspace(
    var value: WorkspaceState,
) : WorkspaceRepository {
    override suspend fun load(): WorkspaceState = value

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState =
        transform(value).also {
            value =
                it
        }
}

private class FakeLive(
    private val outcome: DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    var calls = 0

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        calls++
        return outcome
    }
}

private class FakeExternal(
    private val result: DomainOutcome<ThreadSnapshot> = DomainOutcome.Failure(DomainFailure.NotFound),
) : ExternalThreadArchive {
    var adds = 0
    var loads = 0

    override suspend fun add(
        address: ThreadAddress,
        captcha: String,
    ): ArchiveSubmission {
        adds++
        return ArchiveSubmission.Rejected("test")
    }

    override suspend fun load(
        address: ThreadAddress,
        url: String,
    ): DomainOutcome<ThreadSnapshot> {
        loads++
        return result
    }
}
