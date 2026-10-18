package com.solthariel.pawchan

import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaDownloadResult
import com.solthariel.pawchan.media.MediaDownloader
import com.solthariel.pawchan.media.safeMediaFileName
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class MediaDownloadTest {
    @Test
    fun streamsSuccessfulResponseToDestination() =
        runTest {
            val payload = "media-payload".encodeToByteArray()
            val client = clientReturning(code = 200, payload = payload)
            val destination = ByteArrayOutputStream()

            val result = MediaDownloader(client).download("https://2ch.su/media/file", destination)

            assertEquals(MediaDownloadResult.Success, result)
            assertArrayEquals(payload, destination.toByteArray())
        }

    @Test
    fun doesNotWriteHttpErrorBody() =
        runTest {
            val client = clientReturning(code = 404, payload = "not found".encodeToByteArray())
            val destination = ByteArrayOutputStream()

            val result = MediaDownloader(client).download("https://2ch.su/media/missing", destination)

            assertEquals(MediaDownloadResult.PermanentFailure, result)
            assertEquals(0, destination.size())
        }

    @Test
    fun treatsServerErrorAsRetryable() =
        runTest {
            val client = clientReturning(code = 503, payload = byteArrayOf())

            val result =
                MediaDownloader(client).download(
                    "https://2ch.su/media/unavailable",
                    ByteArrayOutputStream(),
                )

            assertEquals(MediaDownloadResult.RetryableFailure, result)
        }

    @Test
    fun stripsPathSegmentsFromSuggestedFileName() {
        val attachment = attachment("../unsafe\\cat.webm")

        assertEquals("cat.webm", safeMediaFileName(attachment))
    }

    @Test
    fun fallsBackToStableAttachmentIdForBlankName() {
        val attachment = attachment("   ")

        assertEquals("media-attachment-id", safeMediaFileName(attachment))
    }

    private fun clientReturning(
        code: Int,
        payload: ByteArray,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(payload.toResponseBody())
                    .build()
            }.build()

    private fun attachment(fileName: String): MediaAttachmentUi =
        MediaAttachmentUi(
            source =
                MediaAttachmentSource(
                    id = "attachment-id",
                    fileName = fileName,
                    originalUrl = "https://2ch.su/media/file",
                    thumbnailUrl = null,
                ),
            byteSize = null,
            viewer = AttachmentViewer.NONE,
        )
}
