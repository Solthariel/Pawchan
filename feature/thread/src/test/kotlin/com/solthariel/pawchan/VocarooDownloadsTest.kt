package com.solthariel.pawchan

import com.solthariel.pawchan.thread.vocaroo.VocarooDownloads
import com.solthariel.pawchan.thread.vocaroo.VocarooHttpException
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import com.solthariel.pawchan.thread.vocaroo.VocarooRetryPolicy
import com.solthariel.pawchan.thread.vocaroo.VocarooTransport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class VocarooDownloadsTest {
    private val directory: File = Files.createTempDirectory("vocaroo").toFile()

    private fun downloads(
        transport: VocarooTransport,
        policy: VocarooRetryPolicy =
            VocarooRetryPolicy(budgetMillis = 500L, maxAttemptMillis = 200L, minAttemptMillis = 10L),
    ) = VocarooDownloads(transport, directory, policy, log = {})

    @Test fun failedAttemptsAreRetriedSilentlyUntilTheRecordArrives() =
        runBlocking {
            val attempts = mutableListOf<Int>()
            val transport =
                VocarooTransport { request: VocarooRequest, target: File, _ ->
                    attempts += request.attempt
                    if (request.attempt < 2) throw IOException("reset")
                    target.writeText("mp3")
                }
            val file = downloads(transport, VocarooRetryPolicy(budgetMillis = 5_000L)).fetch("abcdef") {}
            assertNotNull(file)
            assertEquals(listOf(1, 2), attempts)
            assertEquals("mp3", requireNotNull(file).readText())
            assertFalse(File(directory, "abcdef.part").exists())
        }

    @Test fun missingRecordIsNotRetried() =
        runBlocking {
            var attempts = 0
            val transport =
                VocarooTransport { _, _, _ ->
                    attempts++
                    throw VocarooHttpException(404)
                }
            assertNull(downloads(transport).fetch("abcdef") {})
            assertEquals(1, attempts)
        }

    @Test fun cancelledDownloadRemovesItsTemporaryFile() =
        runBlocking {
            val started = CompletableDeferred<File>()
            val transport =
                VocarooTransport { _, target, _ ->
                    target.writeText("partial")
                    started.complete(target)
                    awaitCancellation()
                }
            val job = launch(Dispatchers.IO) { downloads(transport).fetch("abcdef") {} }
            val partial = started.await()
            assertTrue(partial.exists())
            job.cancelAndJoin()
            assertFalse(partial.exists())
            assertFalse(File(directory, "abcdef.mp3").exists())
        }

    @Test fun concurrentDownloadsOfSameRecordUseSeparateTemporaryFiles() =
        runBlocking {
            val first = CompletableDeferred<File>()
            val second = CompletableDeferred<File>()
            val transport =
                VocarooTransport { request, target, _ ->
                    target.writeText("partial")
                    if (request.attempt == 1 && !first.isCompleted) first.complete(target) else second.complete(target)
                    awaitCancellation()
                }
            val loader = downloads(transport)
            val a = launch(Dispatchers.IO) { loader.fetch("abcdef") {} }
            val firstFile = first.await()
            val b = launch(Dispatchers.IO) { loader.fetch("abcdef") {} }
            val secondFile = second.await()
            assertTrue(firstFile != secondFile)
            a.cancelAndJoin()
            assertFalse(firstFile.exists())
            assertTrue(secondFile.exists())
            b.cancelAndJoin()
            assertFalse(secondFile.exists())
        }

    @Test fun retryPolicyKeepsPausesAndBudget() {
        val policy = VocarooRetryPolicy()
        assertEquals(1_000L, policy.pauseMillis(1))
        assertEquals(2_000L, policy.pauseMillis(3))
        assertTrue(policy.shouldRetry(IOException(), 10_000L))
        assertTrue(policy.shouldRetry(VocarooHttpException(503), 10_000L))
        assertTrue(policy.shouldRetry(VocarooHttpException(429), 10_000L))
        assertFalse(policy.shouldRetry(VocarooHttpException(410), 10_000L))
        assertFalse(policy.shouldRetry(IOException(), 39_000L))
        assertEquals(20_000L, policy.attemptMillis(0L))
        assertEquals(3_000L, policy.attemptMillis(39_500L))
    }
}
