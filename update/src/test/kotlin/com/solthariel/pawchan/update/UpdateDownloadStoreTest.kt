package com.solthariel.pawchan.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateDownloadStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun onlyVerifiedBytesReplaceTheExistingCandidate() =
        runTest {
            val directory = temporary.newFolder()
            val target = File(directory, "Pawchan-update.apk").apply { writeText("old") }
            var verified = false
            val store =
                UpdateDownloadStore(directory) { file, _ ->
                    assertEquals("new", file.readText())
                    assertEquals("old", target.readText())
                    verified = true
                }
            assertEquals(target, store.download(release()) { it.writeText("new") })
            assertTrue(verified)
            assertEquals("new", target.readText())
            assertEquals(listOf(target), directory.listFiles()?.toList())
        }

    @Test fun failedVerificationPreservesOldApkAndDeletesPartial() =
        runTest {
            val directory = temporary.newFolder()
            val target = File(directory, "Pawchan-update.apk").apply { writeText("old") }
            val store = UpdateDownloadStore(directory) { _, _ -> error("Invalid signer") }
            val result = runCatching { store.download(release()) { it.writeText("bad") } }
            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertEquals("old", target.readText())
            assertEquals(listOf(target), directory.listFiles()?.toList())
        }

    @Test fun transferFailureNeverCallsVerifierAndCleansPartial() =
        runTest {
            val directory = temporary.newFolder()
            val store = UpdateDownloadStore(directory) { _, _ -> error("Must not verify") }
            val result =
                runCatching {
                    store.download(release()) {
                        it.writeText("partial")
                        throw IOException("Connection lost")
                    }
                }
            assertTrue(result.exceptionOrNull() is IOException)
            assertTrue(directory.listFiles().isNullOrEmpty())
        }

    @Test fun cancellationCleansPartialAndPreservesCandidate() =
        runTest {
            val directory = temporary.newFolder()
            val target = File(directory, "Pawchan-update.apk").apply { writeText("old") }
            val started = CompletableDeferred<Unit>()
            val store = UpdateDownloadStore(directory) { _, _ -> error("Must not verify") }
            val job =
                launch {
                    store.download(release()) {
                        it.writeText("partial")
                        started.complete(Unit)
                        awaitCancellation()
                    }
                }
            started.await()
            job.cancelAndJoin()
            assertEquals("old", target.readText())
            assertEquals(listOf(target), directory.listFiles()?.toList())
        }

    @Test fun attemptsNeverShareOrDeleteEachOthersPartialFile() =
        runTest {
            val directory = temporary.newFolder()
            val firstPath = CompletableDeferred<File>()
            val secondPath = CompletableDeferred<File>()
            val store = UpdateDownloadStore(directory) { _, _ -> }
            val first =
                launch {
                    store.download(release()) {
                        firstPath.complete(it)
                        awaitCancellation()
                    }
                }
            val second =
                launch {
                    store.download(release()) {
                        secondPath.complete(it)
                        awaitCancellation()
                    }
                }
            val a = firstPath.await()
            val b = secondPath.await()
            assertNotEquals(a, b)
            first.cancelAndJoin()
            assertFalse(a.exists())
            assertTrue(b.exists())
            second.cancelAndJoin()
            assertTrue(directory.listFiles().isNullOrEmpty())
        }

    @Test fun failedAtomicReplacementDoesNotLeavePartialFiles() =
        runTest {
            val directory = temporary.newFolder()
            val target =
                File(directory, "Pawchan-update.apk").apply {
                    mkdir()
                    resolve("keep").writeText("old")
                }
            val store = UpdateDownloadStore(directory) { _, _ -> }
            val result = runCatching { store.download(release()) { it.writeText("new") } }
            assertTrue(result.isFailure)
            assertEquals("old", target.resolve("keep").readText())
            assertEquals(listOf(target), directory.listFiles()?.toList())
        }

    @Test fun invalidLengthDoesNotStartTransfer() =
        runTest {
            val directory = temporary.newFolder()
            val store = UpdateDownloadStore(directory) { _, _ -> }
            val result =
                async {
                    runCatching { store.download(release().apply { length = MAX_APK_BYTES + 1 }) { error("Must not transfer") } }
                }.await()
            assertTrue(result.exceptionOrNull() is IllegalStateException)
            assertTrue(directory.listFiles().isNullOrEmpty())
        }

    @Test fun cancellationDuringVerificationCannotReplaceTheInstallCandidate() =
        runTest {
            val directory = temporary.newFolder()
            val target = File(directory, "Pawchan-update.apk").apply { writeText("old") }
            val verifying = CountDownLatch(1)
            val finishVerification = CountDownLatch(1)
            val store =
                UpdateDownloadStore(directory) { _, _ ->
                    verifying.countDown()
                    check(finishVerification.await(5, TimeUnit.SECONDS))
                }
            val job = launch { store.download(release()) { it.writeText("new") } }
            try {
                withContext(Dispatchers.IO) { assertTrue(verifying.await(5, TimeUnit.SECONDS)) }
                job.cancel()
            } finally {
                finishVerification.countDown()
                job.cancelAndJoin()
            }
            assertEquals("old", target.readText())
            assertEquals(listOf(target), directory.listFiles()?.toList())
        }

    private fun release() = UpdatePackage().apply { length = 3 }
}
