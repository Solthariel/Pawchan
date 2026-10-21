package com.solthariel.pawchan.settings

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ChangelogHistoryControllerTest {
    @get:Rule val folder = TemporaryFolder()
    private val bundled = { decodeRemoteChangelog(historyBody(1)) }

    @Test fun discoveredVersionUpdatesTheLiveHistoryWithoutRestarting() =
        runTest {
            var published = 1
            val repository =
                ChangelogHistoryRepository(cache(), bundled) { tag, minimum ->
                    if (published == 2) {
                        assertNull(tag)
                        assertEquals(2L, minimum)
                    }
                    val codes = if (published == 1) intArrayOf(1) else intArrayOf(2, 1)
                    ChangelogDownload(historyBody(*codes), "tag$published")
                }
            val controller = ChangelogHistoryController(repository, backgroundScope)
            controller.open()
            assertEquals(
                listOf(1),
                controller.state.value.releases
                    ?.map { it.code },
            )
            published = 2
            controller.updateFound(2)
            val updated = controller.state.first { it.releases?.first()?.code == 2 }
            assertEquals(listOf(2, 1), updated.releases?.map { it.code })
            assertEquals(listOf(2, 1), repository.local().map { it.code })
        }

    @Test fun openingWhileDiscoveryIsInFlightDoesNotReplaceTheNewHistoryWithLocalData() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val response = CompletableDeferred<ChangelogDownload>()
            val repository =
                ChangelogHistoryRepository(cache(), bundled) { _, _ ->
                    started.complete(Unit)
                    response.await()
                }
            val controller = ChangelogHistoryController(repository, backgroundScope)
            controller.updateFound(2)
            started.await()
            val opened = async { controller.open() }
            response.complete(ChangelogDownload(historyBody(2, 1), "tag2"))
            opened.await()
            assertEquals(
                listOf(2, 1),
                controller.state.value.releases
                    ?.map { it.code },
            )
            assertFalse(controller.state.value.failed)
        }

    @Test fun staleResponseAfterDiscoveryIsRejectedAndOpeningRetriesTheExpectedVersion() =
        runTest {
            val saved = cache().apply { save(ChangelogDownload(historyBody(1), "tag1")) }
            var calls = 0
            val requested = CompletableDeferred<Unit>()
            val repository =
                ChangelogHistoryRepository(saved, bundled) { tag, minimum ->
                    assertNull(tag)
                    assertEquals(2L, minimum)
                    calls++
                    if (calls == 1) {
                        requested.complete(Unit)
                        ChangelogDownload(historyBody(1), "stale")
                    } else {
                        ChangelogDownload(historyBody(2, 1), "tag2")
                    }
                }
            val controller = ChangelogHistoryController(repository, backgroundScope)
            controller.updateFound(2)
            requested.await()
            controller.open()
            assertEquals(
                listOf(2, 1),
                controller.state.value.releases
                    ?.map { it.code },
            )
            assertEquals("tag2", saved.load()?.etag)
        }

    @Test fun offlineOpeningKeepsAllPreviouslyDownloadedVersions() =
        runTest {
            val saved = cache().apply { save(ChangelogDownload(historyBody(2, 1), "tag2")) }
            val repository = ChangelogHistoryRepository(saved, bundled) { _, _ -> throw IOException("offline") }
            val controller = ChangelogHistoryController(repository, backgroundScope)
            controller.updateFound(3)
            controller.open()
            assertEquals(
                listOf(2, 1),
                controller.state.value.releases
                    ?.map { it.code },
            )
            assertFalse(controller.state.value.failed)
        }

    @Test fun enteringPrefetchesWithoutOpeningAndRepeatedOpeningDoesNotDownloadAgain() =
        runTest {
            var calls = 0
            val repository =
                ChangelogHistoryRepository(cache(), bundled) { _, _ ->
                    calls++
                    ChangelogDownload(historyBody(2, 1), "tag2")
                }
            val controller = ChangelogHistoryController(repository, backgroundScope)
            val visit = Any()
            controller.enter(visit)
            controller.state.first { it.releases?.first()?.code == 2 }
            assertEquals(1, calls)
            controller.enter(visit)
            controller.updateFound(2)
            controller.open()
            controller.open()
            assertEquals(1, calls)
            controller.enter(Any())
            controller.open()
            assertEquals(2, calls)
        }

    private fun cache() = ChangelogCache(File(folder.root, "history.json"), "https://example.org/history.json")
}
