package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.storePost
import com.solthariel.pawchan.domain.workspace.withRoutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class WorkspaceRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    private fun post(): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(101))),
            PostContent(
                PostDocument(
                    listOf(
                        PostTextRun(
                            "formatted",
                            setOf(PostTextStyle.BOLD),
                            PostLink.Reference(requireNotNull(PostId.parse(99)), address),
                        ),
                    ),
                ),
                PostMetadata("name", "date", true, "subject"),
            ),
            emptyList(),
        )

    @Test fun roundTripKeepsPostFormattingReferenceAndSession() =
        runTest {
            val directory = temporary.newFolder()
            val repository = FileWorkspaceRepository(directory)
            repository.update {
                it
                    .storePost(
                        post(),
                        "title",
                        true,
                    ).copy(tabs = listOf(OpenThread(address, "title", 101, 25)))
                    .withRoutes(last = "thread/dvach/b/100")
            }
            val loaded = FileWorkspaceRepository(directory).load()
            assertEquals("thread/dvach/b/100", loaded.lastRoute)
            assertEquals(25, loaded.tabs.single().offset)
            assertEquals(
                post().content.document.runs,
                loaded.posts
                    .single()
                    .post.content.document.runs,
            )
            assertEquals(
                "subject",
                loaded.posts
                    .single()
                    .post.content.metadata.subject,
            )
        }

    @Test fun concurrentRepositoryInstancesMergeInsteadOfOverwriting() =
        runTest {
            val directory = temporary.newFolder()
            val first = FileWorkspaceRepository(directory)
            val second = FileWorkspaceRepository(directory)
            val one = launch { first.update { it.withRoutes(last = "boards") } }
            val two = launch { second.update { it.storePost(post(), "title") } }
            one.join()
            two.join()
            val loaded = first.load()
            assertEquals("boards", loaded.lastRoute)
            assertEquals(1, loaded.posts.size)
        }

    @Test fun damagedWorkspaceIsNotOverwrittenByUpdate() =
        runTest {
            val directory = temporary.newFolder()
            val file = File(directory, "workspace.json")
            file.writeText("damaged")
            try {
                FileWorkspaceRepository(directory).update { WorkspaceState() }
                fail("Expected IOException")
            } catch (_: IOException) {
                assertEquals("damaged", file.readText())
            }
        }

    @Test fun twoOwnersMergeSnapshotsWithoutDroppingPosts() =
        runTest {
            val directory = temporary.newFolder()
            val a = FileThreadHistoryRepository(directory)
            val b = FileThreadHistoryRepository(directory)
            kotlinx.coroutines.coroutineScope {
                listOf(a, b).forEachIndexed { index, owner ->
                    launch(kotlinx.coroutines.Dispatchers.IO) {
                        val sample = post()
                        val postAddress = PostAddress(address, requireNotNull(PostId.parse(101L + index)))
                        val row = BoardPost(postAddress, sample.content, emptyList())
                        val snapshot = ThreadSnapshot(address, "test", ThreadFlags(false, false, false), listOf(row))
                        owner.mergeAndSave(snapshot)
                    }
                }
            }
            assertEquals(setOf(101L, 102L), requireNotNull(a.load(address)).posts.map { it.address.post.value }.toSet())
        }

    @Test fun onlyMarkedRunsRetainTheirBlockIdentityInNewCopies() {
        val base = post()
        val document = PostDocument(listOf(PostTextRun("@monkey вопрос\n"), PostTextRun("ответ\n", monkeyBlock = 1)))
        val mixed = BoardPost(base.address, base.content.copy(document = document), emptyList())
        assertEquals(
            document.runs,
            mixed
                .encodePost()
                .decodePost()
                .content.document.runs,
        )
        val plain =
            base
                .encodePost()
                .decodePost()
                .content.document
        assertTrue(plain.runs.none { it.monkeyBlock != null })
    }

    @Test fun offlineSnapshotSurvivesRestart() =
        runTest {
            val repository = FileWorkspaceRepository(temporary.newFolder())
            val snapshot =
                ThreadSnapshot(
                    address,
                    "title",
                    ThreadFlags(true, false, true),
                    listOf(post()),
                    subjectsEnabled = false,
                )
            repository.update { it.copy(copies = listOf(ThreadCopy(snapshot, 123))) }
            val restored = repository.load().copies.single()
            assertEquals(123L, restored.savedAt)
            assertEquals(false, restored.snapshot.subjectsEnabled)
            assertTrue(restored.snapshot.flags.archived)
            assertEquals(
                "formatted",
                restored.snapshot.posts
                    .single()
                    .content.document.plainText,
            )
        }
}
