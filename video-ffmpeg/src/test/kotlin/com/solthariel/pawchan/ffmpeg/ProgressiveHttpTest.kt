package com.solthariel.pawchan.ffmpeg

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProgressiveHttpTest {
    @Test fun mediaRequestAsksForUnencodedBytes() {
        assertEquals("identity", mediaRequest("https://example.test/v.mp4").build().header("Accept-Encoding"))
    }

    @Test fun weakEtagFallsBackToLastModified() {
        val tag = EntityTag.of(response(200, "ETag" to "W/\"weak\"", "Last-Modified" to "Tue, 03 Nov 2026"))

        assertEquals("Tue, 03 Nov 2026", tag.ifRange)
    }

    @Test fun missingValidatorsDisableRanges() {
        assertNull(EntityTag.of(response(200)).ifRange)
    }

    @Test fun primaryResponseMustBeUnencodedOk() {
        checkedPrimaryBody(response(200, body = "abc"))
        assertThrows(IOException::class.java) { checkedPrimaryBody(response(206, body = "abc")) }
        assertThrows(IOException::class.java) {
            checkedPrimaryBody(response(200, "Content-Encoding" to "gzip", body = "abc"))
        }
    }

    @Test fun onlyOkAndUnsatisfiableRefuseRanges() {
        assertTrue(rangeRefused(response(200)))
        assertTrue(rangeRefused(response(416)))
        assertFalse(rangeRefused(response(206)))
    }

    @Test fun verifiedRangeReturnsExclusiveEnd() {
        val tag = EntityTag("\"same\"", null)
        val partial = response(206, "ETag" to "\"same\"", "Content-Range" to "bytes 4-7/8", body = "abcd")

        assertEquals(8L, checkedRangeEnd(partial, 4, 8, tag))
    }

    @Test fun changedEntityOrLengthMismatchIsRejected() {
        val tag = EntityTag("\"same\"", null)
        val changed = response(206, "ETag" to "\"other\"", "Content-Range" to "bytes 4-7/8", body = "abcd")
        val shortBody = response(206, "ETag" to "\"same\"", "Content-Range" to "bytes 4-7/8", body = "ab")
        val wrongStatus = response(200, "ETag" to "\"same\"", "Content-Range" to "bytes 4-7/8", body = "abcd")

        assertThrows(IOException::class.java) { checkedRangeEnd(changed, 4, 8, tag) }
        assertThrows(IOException::class.java) { checkedRangeEnd(shortBody, 4, 8, tag) }
        assertThrows(IOException::class.java) { checkedRangeEnd(wrongStatus, 4, 8, tag) }
    }

    private fun response(
        code: Int,
        vararg headers: Pair<String, String>,
        body: String = "",
    ): Response =
        Response
            .Builder()
            .request(Request.Builder().url("https://example.test/v.mp4").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .body(body.toResponseBody())
            .build()
}
