package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingUploadProgress
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.ForwardingSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PostingUploadTrackerTest {
    @Test fun reportsAggregateMediaBytesWithoutMultipartOverheadAndResetsOnReplay() {
        val events = mutableListOf<PostingUploadProgress>()
        val tracker =
            PostingUploadTracker(listOf(16_384, 8_192), PostingProgressObserver(events::add)) {
                events.size.toLong() * 100_000_000L
            }
        val body =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("comment", "not media")
                .addFormDataPart("file[]", "a", tracker.file(0, ByteArray(16_384)))
                .addFormDataPart("file[]", "b", tracker.file(1, ByteArray(8_192)))
                .build()
        val request = tracker.request(body)
        repeat(2) {
            val start = events.size
            request.writeTo(Buffer())
            assertEquals(0L, events[start].sentBytes)
            assertEquals(24_576L, events.last().sentBytes)
            assertEquals(24_576L, events.last().totalBytes)
            assertTrue(events.last().awaitingResponse)
            assertTrue(events.drop(start).any { it.sentBytes in 1 until it.totalBytes })
            assertTrue(events.drop(start).zipWithNext().all { (a, b) -> a.sentBytes <= b.sentBytes })
        }
    }

    @Test fun throttlesFrequentWritesButAlwaysReportsCompletion() {
        val events = mutableListOf<PostingUploadProgress>()
        val tracker = PostingUploadTracker(listOf(32_768), PostingProgressObserver(events::add)) { 1L }
        val request = tracker.request(tracker.file(0, ByteArray(32_768)))
        request.writeTo(Buffer())
        assertEquals(2, events.size)
        assertEquals(100, events.last().percent)
    }

    @Test fun failedWriteNeverReportsWaitingOrServerSuccess() {
        val events = mutableListOf<PostingUploadProgress>()
        val tracker = PostingUploadTracker(listOf(16_384), PostingProgressObserver(events::add))
        val broken =
            object : ForwardingSink(Buffer()) {
                override fun write(
                    source: Buffer,
                    byteCount: Long,
                ): Unit = throw IOException("transport closed")
            }.buffer()
        try {
            tracker.request(tracker.file(0, ByteArray(16_384))).writeTo(broken)
            throw AssertionError("Expected transport failure")
        } catch (_: IOException) {
            assertFalse(events.any { it.awaitingResponse })
        }
    }

    @Test fun textOnlyRequestHasNoInventedPercentage() {
        val events = mutableListOf<PostingUploadProgress>()
        PostingUploadTracker(emptyList(), PostingProgressObserver(events::add))
            .request("comment".toRequestBody())
            .writeTo(Buffer())
        assertEquals(0L, events.last().totalBytes)
        assertTrue(events.last().awaitingResponse)
    }
}
