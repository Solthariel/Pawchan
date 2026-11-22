package com.solthariel.pawchan

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Real libmpv and two Android surfaces; this is not a test of Android's PiP window itself. */
class MpvSurfaceHandoffRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pausedSoftwarePlaybackRedrawsOnReplacementSurfaces() {
        verifyPausedTransfers(software = true)
    }

    @Test fun pausedAutoPlaybackRedrawsOnReplacementSurfaces() {
        verifyPausedTransfers(software = false)
    }

    private fun verifyPausedTransfers(software: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("mpv-surface-handoff", ".mp4", context.cacheDir)
        instrumentation.context.assets.open("video/h264_aac.mp4").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        var engine: MpvVideoEngine? = null
        var first: FrameLayout? = null
        var second: FrameLayout? = null
        val frames = AtomicInteger()
        val failed = AtomicBoolean()
        val last = AtomicReference(VideoPlaybackStatus())
        try {
            compose.setContent {
                Row(Modifier.fillMaxSize()) {
                    AndroidView(factory = { FrameLayout(it).also { view -> first = view } }, modifier = Modifier.weight(1f).fillMaxHeight())
                    AndroidView(factory = {
                        FrameLayout(it).also { view ->
                            second = view
                        }
                    }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
            compose.runOnIdle {
                engine = MpvVideoEngine(context, OkHttpClient(), context.cacheDir)
                checkNotNull(engine).attach(checkNotNull(first))
                checkNotNull(engine).open(file.toURI().toString(), software) { event, status ->
                    last.set(status)
                    if (event == VideoEvent.VIDEO_OUTPUT) frames.incrementAndGet()
                    if (event == VideoEvent.ERROR) failed.set(true)
                }
                checkNotNull(engine).pause()
            }
            compose.waitUntil(20_000) { failed.get() || (frames.get() >= 1 && last.get().seekable) }
            assertFalse("mpv could not open fixture", failed.get())
            assertTrue("mpv produced no frame", frames.get() >= 1)
            compose.runOnIdle { checkNotNull(engine).seekTo(600) }
            compose.waitUntil(10_000) { failed.get() || last.get().position in 450..850 }
            assertFalse("mpv seek failed", failed.get())
            assertTrue("seek did not reach requested position", last.get().position in 450..850)
            val beforeTransfer = frames.get()
            compose.runOnIdle { checkNotNull(engine).attach(checkNotNull(second)) }
            compose.waitUntil(15_000) { failed.get() || frames.get() > beforeTransfer }
            assertFalse("mpv failed on the replacement surface", failed.get())
            assertTrue("paused frame was not rendered again", frames.get() > beforeTransfer)
            assertFalse("handoff unexpectedly resumed playback", last.get().playing)
            assertTrue("handoff changed playback position", last.get().position in 450..850)
            repeat(3) { index ->
                val previousFrames = frames.get()
                val nextSurface = if (index % 2 == 0) first else second
                compose.runOnIdle { checkNotNull(engine).attach(checkNotNull(nextSurface)) }
                compose.waitUntil(15_000) { failed.get() || frames.get() > previousFrames }
                assertFalse("mpv failed during repeated surface transfer", failed.get())
                assertTrue("paused frame was not redrawn", frames.get() > previousFrames)
                assertFalse("transfer unexpectedly resumed playback", last.get().playing)
                assertTrue("transfer changed playback position", last.get().position in 450..850)
            }
        } finally {
            compose.runOnIdle { engine?.close() }
            file.delete()
        }
    }
}
