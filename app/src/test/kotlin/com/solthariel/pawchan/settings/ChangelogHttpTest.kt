package com.solthariel.pawchan.settings

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class ChangelogHttpTest {
    @Test fun sendsValidatorAndReadsTheNewBodyAndTag() =
        runTest {
            val client = client(200, historyBody(1)) { assertEquals("saved", it) }
            val response = ChangelogHttp(client).download("https://example.org/history.json", "saved")
            assertEquals(historyBody(1), response.body)
            assertEquals("new", response.etag)
        }

    @Test fun notModifiedDoesNotReadOrReplaceTheBody() =
        runTest {
            val response = ChangelogHttp(client(304, "")).download("https://example.org/history.json", "saved")
            assertNull(response.body)
            assertEquals("saved", response.etag)
        }

    @Test(expected = IOException::class)
    fun serverFailureIsNotAValidHistory() =
        runTest {
            ChangelogHttp(client(503, "unavailable")).download("https://example.org/history.json", null)
            Unit
        }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedDownloadIsRejectedBeforeParsing() =
        runTest {
            ChangelogHttp(client(200, " ".repeat(MAX_HISTORY_BYTES + 1)))
                .download("https://example.org/history.json", null)
            Unit
        }

    private fun client(
        code: Int,
        body: String,
        checkTag: (String?) -> Unit = {},
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                checkTag(chain.request().header("If-None-Match"))
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("Test")
                    .header("ETag", "new")
                    .body(body.toResponseBody())
                    .build()
            }.build()
}
