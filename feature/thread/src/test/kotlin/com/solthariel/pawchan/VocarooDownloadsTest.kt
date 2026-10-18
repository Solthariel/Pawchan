package com.solthariel.pawchan

import com.solthariel.pawchan.thread.vocaroo.VocarooDownloads
import com.solthariel.pawchan.thread.vocaroo.VocarooHttpException
import com.solthariel.pawchan.thread.vocaroo.VocarooRequest
import com.solthariel.pawchan.thread.vocaroo.VocarooRetryPolicy
import com.solthariel.pawchan.thread.vocaroo.VocarooTransport
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
