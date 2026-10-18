package com.solthariel.pawchan

import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.attachmentImagePreloadUrls
import com.solthariel.pawchan.media.preloadAttachmentImages
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentImagePreloadTest {
    @Test
    fun includesOnlyImagesAndDeduplicatesOriginalUrls() {
        val items =
            listOf(
                attachment("still"),
                attachment("video", viewer = AttachmentViewer.VIDEO),
                attachment("still"),
                attachment("gif"),
                attachment("file", viewer = AttachmentViewer.NONE),
            )
        assertEquals(listOf("https://example.com/still", "https://example.com/gif"), attachmentImagePreloadUrls(items))
    }

    @Test
    fun skipsUnknownInvalidAndOversizedFilesWithoutBlockingLaterImages() {
        val items =
            listOf(
                attachment("unknown", null),
                attachment("zero", 0),
                attachment("negative", -1),
                attachment("huge", 17 * MIB),
                attachment("", 1).copy(source = MediaAttachmentSource("blank", "blank", "", null)),
                attachment("small"),
            )
        assertEquals(listOf("https://example.com/small"), attachmentImagePreloadUrls(items))
    }

    @Test
    fun limitsTotalMetadataBytesAndDoesNotChargeDuplicatesTwice() {
        val items =
            listOf(
                attachment("a", 16 * MIB),
                attachment("a", 16 * MIB),
                attachment("b", 16 * MIB),
                attachment("c"),
            )
        assertEquals(listOf("https://example.com/a", "https://example.com/b"), attachmentImagePreloadUrls(items))
    }

    @Test
    fun skipsFileExceedingRemainingBudgetAndIncludesSmallerImage() {
        val items =
            listOf(
                attachment("a", 16 * MIB),
                attachment("b", 15 * MIB),
                attachment("tooLarge", 2 * MIB),
                attachment("fits", MIB),
            )
        val expected = listOf("https://example.com/a", "https://example.com/b", "https://example.com/fits")
        assertEquals(expected, attachmentImagePreloadUrls(items))
    }

    @Test
    fun limitsBatchToFourImages() {
        val items = (1..8).map { attachment("$it") }
        assertEquals(items.take(4).map { it.originalUrl }, attachmentImagePreloadUrls(items))
    }

    @Test
    fun runsRequestsSequentiallyWithoutRetrying() =
        runTest {
            val calls = mutableListOf<String>()
            var active = 0
            preloadAttachmentImages(listOf(attachment("a"), attachment("b"))) { url ->
                active += 1
                assertEquals(1, active)
                calls += url
                kotlinx.coroutines.yield()
                active -= 1
            }
            assertEquals(listOf("https://example.com/a", "https://example.com/b"), calls)
        }

    @Test
    fun cancellingOwnerCancelsActiveLoadAndNeverStartsNextImage() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val calls = mutableListOf<String>()
            var cancelled = false
            val owner =
                launch {
                    preloadAttachmentImages(listOf(attachment("a"), attachment("b"))) { url ->
                        calls += url
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                }
            started.await()
            owner.cancelAndJoin()
            assertTrue(cancelled)
            assertEquals(listOf("https://example.com/a"), calls)
        }

    private fun attachment(
        id: String,
        bytes: Long? = 1024,
        viewer: AttachmentViewer = AttachmentViewer.IMAGE,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.png", "https://example.com/$id", "https://example.com/thumb/$id"),
            bytes,
            viewer,
        )

    private companion object {
        const val MIB = 1024L * 1024
    }
}
