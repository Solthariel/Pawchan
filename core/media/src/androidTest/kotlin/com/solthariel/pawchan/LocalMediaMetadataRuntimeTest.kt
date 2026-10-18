package com.solthariel.pawchan

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.metadata.localMediaMetadataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaMetadataRuntimeTest {
    @Test fun imageUsesViewerSummaryAndRemovesTemporaryFile() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val before = temporaryFiles()
            val bytes = Base64.decode(PREVIEW_PNG, Base64.DEFAULT)
            val metadata = localMediaMetadataSource(context, bytes, video = false).load()
            assertNotNull(metadata)
            assertTrue(requireNotNull(metadata).summary.startsWith("PNG · 1×1 · "))
            assertEquals(before, temporaryFiles())
        }

    @Test fun videoUsesRealCodecResolutionAndDuration() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val bytes =
                instrumentation.context.assets
                    .open("video/h264_aac.mp4")
                    .use { it.readBytes() }
            val before = temporaryFiles()
            val metadata = localMediaMetadataSource(instrumentation.targetContext, bytes, video = true).load()
            assertNotNull(metadata)
            val details = requireNotNull(metadata)
            assertTrue(details.summary.startsWith("H.264 · "))
            assertTrue(details.sections.flatMap { it.rows }.any { it.label == "Длительность" })
            assertEquals(before, temporaryFiles())
        }

    @Test fun unreadableVideoFallsBackWithoutLeavingTemporaryFiles() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val before = temporaryFiles()
            assertNull(localMediaMetadataSource(context, byteArrayOf(1, 2, 3), video = true).load())
            assertEquals(before, temporaryFiles())
        }

    private fun temporaryFiles(): Set<String> =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext.cacheDir
            .listFiles()
            .orEmpty()
            .filter { it.name.startsWith("attachment-metadata-") }
            .map { it.name }
            .toSet()
}

private const val PREVIEW_PNG =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aHYsAAAAASUVORK5CYII="
