package com.solthariel.pawchan.data.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogCreationTimeTest {
    private val decoder = CatalogPayloadDecoder(Json { ignoreUnknownKeys = true })

    @Test
    fun carriesOpTimestampThroughCatalogReconstruction() {
        assertEquals(1_800_000_000L, decode("1800000000"))
        assertEquals(1_800_000_000L, decode("\"1800000000\""))
    }

    @Test
    fun unknownInvalidOrOverflowingTimeDoesNotDiscardThread() {
        for (timestamp in listOf("null", "0", "-1", "\"bad\"", "{}", "[]", "9223372036854775808")) {
            assertNull(decode(timestamp))
        }
    }

    @Test
    fun missingTimestampDoesNotUseTheFormattedDateOrLastReplyTime() {
        val payload =
            """
            {"board":{"id":"b","name":"Бред"},
             "threads":[{"num":42,"date":"08.10.2026 20:00","last_modified":1800000010}]}
            """.trimIndent()
        assertNull(requireNotNull(decoder.decode(payload)).threads.single().createdAtEpochSeconds)
    }

    private fun decode(timestamp: String): Long? {
        val payload = """{"board":{"id":"b","name":"Бред"},"threads":[{"num":42,"timestamp":$timestamp}]}"""
        return requireNotNull(decoder.decode(payload)).threads.single().createdAtEpochSeconds
    }
}
