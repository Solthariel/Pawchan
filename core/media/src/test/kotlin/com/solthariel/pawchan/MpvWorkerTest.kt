package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.MpvWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class MpvWorkerTest {
    @Test
    fun reportsFailureAndContinuesProcessingInOrder() {
        val error = IOException("read failed")
        val reported = AtomicReference<Triple<Int, String, Throwable>>()
        val complete = CountDownLatch(1)
        val worker = MpvWorker { token, operation, cause -> reported.set(Triple(token, operation, cause)) }
        try {
            worker.execute(7, "open") { throw error }
            worker.execute(8, "next") { complete.countDown() }
            assertTrue("worker did not process the next task", complete.await(5, TimeUnit.SECONDS))
            val result = checkNotNull(reported.get())
            assertEquals(7, result.first)
            assertEquals("open", result.second)
            assertSame(error, result.third)
        } finally {
            worker.shutdown()
        }
    }

    @Test
    fun delayedNativeLinkageFailureIsReported() {
        val error = UnsatisfiedLinkError("native function missing")
        val reported = AtomicReference<Triple<Int, String, Throwable>>()
        val complete = CountDownLatch(1)
        val worker =
            MpvWorker { token, operation, cause ->
                reported.set(Triple(token, operation, cause))
                complete.countDown()
            }
        try {
            worker.schedule(3, "poll", 1) { throw error }
            assertTrue("scheduled failure was lost", complete.await(5, TimeUnit.SECONDS))
            val result = checkNotNull(reported.get())
            assertEquals(3, result.first)
            assertEquals("poll", result.second)
            assertSame(error, result.third)
        } finally {
            worker.shutdown()
        }
    }

    @Test
    fun successfulTasksDoNotReportFailure() {
        val failures = mutableListOf<Throwable>()
        val complete = CountDownLatch(1)
        val worker = MpvWorker { _, _, error -> failures.add(error) }
        try {
            worker.execute(1, "property") { }
            worker.execute(1, "next") { complete.countDown() }
            assertTrue(complete.await(5, TimeUnit.SECONDS))
            assertTrue(failures.isEmpty())
        } finally {
            worker.shutdown()
        }
    }
}
