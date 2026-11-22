package com.solthariel.pawchan

import android.os.SystemClock
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Uses real libmpv to check rapid stop/reopen; AUTO may fall back to software. */
class MpvSessionRestartRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun softwareSessionCanReopen() = verifyRestart(software = true)

    @Test fun autoSessionCanReopen() = verifyRestart(software = false)

    private fun verifyRestart(software: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("mpv-restart", ".mp4", context.cacheDir)
        instrumentation.context.assets.open("video/h264_aac.mp4").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        var host: FrameLayout? = null
        var engine: MpvVideoEngine? = null
        val replaced = AtomicBoolean(false)
        val lateFirstEvents = AtomicInteger()
        val secondEvents = AtomicInteger()
        val firstFrame = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        val status = AtomicReference(VideoPlaybackStatus())
        try {
            compose.setContent {
                AndroidView(factory = { FrameLayout(it).also { view -> host = view } }, modifier = Modifier.fillMaxSize())
            }
            compose.runOnIdle {
                val player = MpvVideoEngine(context, OkHttpClient(), context.cacheDir)
                engine = player
                player.attach(checkNotNull(host))
                player.open(file.toURI().toString(), software) { _, _ ->
                    if (replaced.get()) lateFirstEvents.incrementAndGet()
                }
                replaced.set(true)
                player.open(file.toURI().toString(), software) { event, state ->
                    secondEvents.incrementAndGet()
                    status.set(state)
                    if (event == VideoEvent.VIDEO_OUTPUT) firstFrame.set(true)
                    if (event == VideoEvent.ERROR) failed.set(true)
                }
                player.pause()
            }
            compose.waitUntil(20_000) { failed.get() || (firstFrame.get() && status.get().seekable) }
            assertFalse("reopened mpv session failed", failed.get())
            assertTrue("reopened session did not render", firstFrame.get())
            assertEquals("old session delivered a late event", 0, lateFirstEvents.get())
            compose.runOnIdle { checkNotNull(engine).seekTo(600) }
            compose.waitUntil(10_000) { failed.get() || status.get().position in 450..850 }
            assertFalse("seek after reopen failed", failed.get())
            assertTrue("reopened session did not seek", status.get().position in 450..850)
            compose.runOnIdle { checkNotNull(engine).close() }
            val eventsAtClose = secondEvents.get()
            SystemClock.sleep(500)
            compose.waitForIdle()
            assertEquals("closed session delivered a late event", eventsAtClose, secondEvents.get())
        } finally {
            compose.runOnIdle { engine?.close() }
            file.delete()
        }
    }
}
