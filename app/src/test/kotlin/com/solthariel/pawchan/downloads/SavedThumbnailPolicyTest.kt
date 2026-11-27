package com.solthariel.pawchan.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.FileNotFoundException
import java.util.concurrent.CancellationException

class SavedThumbnailPolicyTest {
    @Test fun providerPreviewIsPreferredWithoutReadingTheOriginal() {
        assertEquals("system", savedThumbnailOrLocal({ "system" }, { error("Unexpected fallback") }))
    }

    @Test fun providerWithoutPreviewsFallsBackOnce() {
        var calls = 0
        assertEquals(
            "frame",
            savedThumbnailOrLocal({ throw FileNotFoundException() }, {
                calls++
                "frame"
            }),
        )
        assertEquals(1, calls)
        assertEquals("image", savedThumbnailOrLocal({ null }, { "image" }))
    }

    @Test fun revokedGrantDoesNotTryAnotherPath() {
        assertNull(savedThumbnailOrLocal<Any>({ throw SecurityException() }, { error("Must not bypass permission") }))
    }

    @Test fun missingFileOrBrokenCodecAreNotDisplayedAsSuccess() {
        assertNull(savedThumbnailOrLocal<Any>({ null }, { throw FileNotFoundException() }))
        assertNull(savedThumbnailOrLocal<Any>({ null }, { throw IllegalStateException("codec") }))
    }

    @Test fun cancellationIsNotAReasonToStartAnotherDecoder() {
        assertThrows(CancellationException::class.java) {
            savedThumbnailOrLocal<Any>({ throw CancellationException() }, { error("Unexpected fallback") })
        }
    }

    @Test fun opaqueMimeFallsBackToExtensionButExplicitMediaMimeWins() {
        assertEquals(SavedPreviewKind.VIDEO, savedPreviewKind("clip.MOV", "application/octet-stream"))
        assertEquals(SavedPreviewKind.VIDEO, savedPreviewKind("no-extension", "video/mp4"))
        assertEquals(SavedPreviewKind.IMAGE, savedPreviewKind("scan.JPEG"))
        assertEquals(SavedPreviewKind.IMAGE, savedPreviewKind("example.mp4", "image/gif"))
        assertEquals(SavedPreviewKind.OTHER, savedPreviewKind("notes.txt", "text/plain"))
    }

    @Test fun boundsAreSmallForPortraitLandscapeAndExtremeAspect() {
        assertEquals(112 to 63, savedThumbnailSize(3840, 2160, 112))
        assertEquals(63 to 112, savedThumbnailSize(2160, 3840, 112))
        assertEquals(1 to 112, savedThumbnailSize(1, Int.MAX_VALUE, 112))
        assertEquals(2 to 3, savedThumbnailSize(2, 3, 112))
        assertNull(savedThumbnailSize(0, 10, 112))
    }
}
