package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerAttachmentViewer
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class ComposerVideoPlayerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun attachmentUsesSharedPlaybackControlsAndRemovesTemporaryFile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bytes =
            instrumentation.context.assets
                .open("video/h264_aac.mp4")
                .use { it.readBytes() }
        val attachment = ComposerAttachment(ReplyAttachment("video.mp4", "video/mp4", bytes))
        assertTrue(attachment.video)
        val previous = previewFiles(context.cacheDir)
        var visible by mutableStateOf(true)
        compose.setContent {
            val loader = remember { ImageLoader.Builder(context).build() }
            DisposableEffect(loader) { onDispose { loader.shutdown() } }
            MaterialTheme {
                if (visible) ComposerAttachmentViewer(attachment, loader, true, {}, {}, {})
            }
        }
        try {
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithContentDescription("Воспроизвести").fetchSemanticsNodes().isNotEmpty()
            }
            // The editor opens paused, as before; playback is started explicitly.
            compose.onNodeWithContentDescription("Воспроизвести").performClick()
            compose.waitUntil(15_000L) {
                compose.onAllNodesWithContentDescription("Пауза").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue((previewFiles(context.cacheDir) - previous).isNotEmpty())
        } finally {
            compose.runOnIdle { visible = false }
            compose.waitUntil(15_000L) { (previewFiles(context.cacheDir) - previous).isEmpty() }
        }
    }
}

private fun previewFiles(cache: File): Set<String> =
    cache
        .listFiles()
        .orEmpty()
        .filter { it.name.startsWith("composer-video-") }
        .map { it.name }
        .toSet()
