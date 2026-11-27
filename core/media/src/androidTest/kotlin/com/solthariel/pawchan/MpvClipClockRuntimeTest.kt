package com.solthariel.pawchan

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Runs with the shipping libmpv, not a test clock: both reads and seeks must use clip-relative time. */
class MpvClipClockRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paddedClipSeeksPausesAndRepeatsRelativeToItsThreeSeconds() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val file = File.createTempFile("mpv-gap-", ".mp4", context.cacheDir)
        val state = AtomicReference(VideoPlaybackStatus())
        val failed = AtomicBoolean()
        val output = AtomicBoolean()
        var engine: MpvVideoEngine? = null
        lateinit var container: FrameLayout

        fun await(condition: () -> Boolean) {
            compose.waitUntil(20_000) { failed.get() || condition() }
            assertFalse("mpv failed: ${state.get()}", failed.get())
        }
        try {
            inst.context.assets.open("video/synthetic-common-gap.mp4").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            compose.setContent {
                AndroidView(factory = { FrameLayout(it).also { box -> container = box } }, modifier = Modifier.fillMaxSize())
            }
            compose.runOnIdle {
                val player = MpvVideoEngine(context, null, context.cacheDir)
                engine = player
                player.attach(container)
                player.muted(true)
                player.open(file.toURI().toString(), software = false) { event, snapshot ->
                    if (event == VideoEvent.ERROR) failed.set(true)
                    if (event == VideoEvent.VIDEO_OUTPUT) output.set(true)
                    state.set(snapshot)
                }
                player.pause()
            }
            await { output.get() && state.get().seekable && !state.get().playing }
            assertTrue("Movie header must not be the clip length: ${state.get()}", state.get().duration in 2900..3200)
            assertTrue(state.get().position in 0..400)
            compose.runOnIdle { checkNotNull(engine).seekTo(1500) }
            await { state.get().position in 1250..1750 && !state.get().playing }
            compose.runOnIdle { checkNotNull(engine).play() }
            await { state.get().ended }
            compose.runOnIdle { checkNotNull(engine).replay() }
            await { state.get().playing && state.get().position in 1..1200 }
            assertTrue(state.get().duration in 2900..3200)
        } finally {
            compose.runOnIdle { engine?.close() }
            file.delete()
        }
    }
}
