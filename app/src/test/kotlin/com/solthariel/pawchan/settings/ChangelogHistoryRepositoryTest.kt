package com.solthariel.pawchan.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ChangelogHistoryRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val url = "https://example.org/feed/history-v1.json"
    private val bundled = { decodeRemoteChangelog(historyBody(1)) }

    @Test fun localHistoryOpensWithoutAnyRequestAndRefreshSavesLaterVersions() =
        runTest {
            val file = File(folder.root, "history.json")
            var calls = 0
            val repository =
                ChangelogHistoryRepository(ChangelogCache(file, url), bundled) {
                    calls++
                    ChangelogDownload(historyBody(2, 1), "tag2")
                }
            assertEquals(listOf(1), repository.local().map { it.code })
            assertEquals(0, calls)
            assertEquals(listOf(2, 1), repository.refresh()?.map { it.code })
            val restarted = ChangelogHistoryRepository(ChangelogCache(file, url), bundled) { throw IOException() }
            assertEquals(listOf(2, 1), restarted.local().map { it.code })
        }

    @Test fun unchangedResponseUsesSavedValidatorAndDoesNotRewriteCache() =
        runTest {
            val file = File(folder.root, "history.json")
            val cache = ChangelogCache(file, url)
            cache.save(ChangelogDownload(historyBody(2, 1), "tag2"))
            val bytes = file.readText()
            val repository =
                ChangelogHistoryRepository(cache, bundled) { tag ->
                    assertEquals("tag2", tag)
                    ChangelogDownload(null, tag)
                }
            assertEquals(listOf(2, 1), repository.refresh()?.map { it.code })
            assertEquals(bytes, file.readText())
        }

    @Test fun interruptedDownloadKeepsTheLastGoodCopy() =
        runTest {
            val cache = savedCache()
            val repository = ChangelogHistoryRepository(cache, bundled) { throw IOException("offline") }
            assertNull(repository.refresh())
            assertEquals(listOf(2, 1), repository.local().map { it.code })
            assertEquals("tag2", cache.load()?.etag)
        }

    @Test fun invalidNewBodyCannotPoisonTheSavedValidator() =
        runTest {
            val cache = savedCache()
            val repository = ChangelogHistoryRepository(cache, bundled) { ChangelogDownload("not JSON", "bad") }
            assertNull(repository.refresh())
            assertEquals("tag2", cache.load()?.etag)
            assertEquals(listOf(2, 1), repository.local().map { it.code })
        }

    @Test fun oldResponseCannotRollHistoryBack() =
        runTest {
            val cache = savedCache()
            val repository = ChangelogHistoryRepository(cache, bundled) { ChangelogDownload(historyBody(1), "old") }
            assertNull(repository.refresh())
            assertEquals("tag2", cache.load()?.etag)
        }

    @Test fun corruptCacheFallsBackToApkAndCanBeRepaired() =
        runTest {
            val file = File(folder.root, "history.json").apply { writeText("broken") }
            val repository =
                ChangelogHistoryRepository(ChangelogCache(file, url), bundled) {
                    assertNull(it)
                    ChangelogDownload(historyBody(2, 1), "new")
                }
            assertEquals(listOf(1), repository.local().map { it.code })
            assertNotNull(repository.refresh())
            assertEquals("new", ChangelogCache(file, url).load()?.etag)
        }

    @Test fun incompleteNewHistoryCannotDiscardOlderDownloadedVersions() =
        runTest {
            val cache = savedCache()
            val repository = ChangelogHistoryRepository(cache, bundled) { ChangelogDownload(historyBody(3), "partial") }
            assertNull(repository.refresh())
            assertEquals("tag2", cache.load()?.etag)
            assertEquals(listOf(2, 1), repository.local().map { it.code })
        }

    @Test fun unchangedServerFileCannotHideTheNewlyInstalledApkVersion() =
        runTest {
            val cache = savedCache()
            val repository =
                ChangelogHistoryRepository(cache, { decodeRemoteChangelog(historyBody(3, 2, 1)) }) {
                    ChangelogDownload(null, "tag2")
                }
            assertEquals(listOf(3, 2, 1), repository.refresh()?.map { it.code })
            assertEquals(listOf(3, 2, 1), repository.local().map { it.code })
        }

    @Test fun cacheFromAnotherFeedIsNotReused() {
        val cache = savedCache()
        assertNotNull(cache.load())
        assertNull(ChangelogCache(File(folder.root, "history.json"), "https://another.org/history.json").load())
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotTreatedAsANetworkError() =
        runTest {
            ChangelogHistoryRepository(savedCache(), bundled) { throw CancellationException() }.refresh()
            Unit
        }

    private fun savedCache(): ChangelogCache =
        ChangelogCache(File(folder.root, "history.json"), url).apply {
            save(ChangelogDownload(historyBody(2, 1), "tag2"))
        }
}
