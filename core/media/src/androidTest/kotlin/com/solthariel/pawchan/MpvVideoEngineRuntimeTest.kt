package com.solthariel.pawchan

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class MpvVideoEngineRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun softwareDecoderPlaysH264AndAac() = playFixture("h264_aac.mp4")

    @Test
    fun softwareDecoderPlaysMpeg4AndAc3() = playFixture("mpeg4_ac3.mp4")

    @Test
    fun softwareDecoderPlaysVp8AndVorbis() = playFixture("vp8_vorbis.webm")

    @Test
    fun softwareDecoderSeeksTruncatedWebm() = playFixture("truncated_seek.webm")

    @Test
    fun detachedTextureCannotReportOutputForReplacement() = playFixture("h264_aac.mp4", checkDetached = true)

    @Test
    fun softwareDecoderAcceptsGenuineBlackFrames() = playFixture("black_h264.mp4")

    @Test
    fun hardwareRequestedDecoderAcceptsGenuineBlackFrames() = playFixture("black_h264.mp4", software = false)

    private fun playFixture(
        name: String,
        checkDetached: Boolean = false,
        software: Boolean = true,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("mpv-fixture-", ".media", context.cacheDir)
        instrumentation.context.assets.open("video/$name").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        lateinit var engine: MpvVideoEngine
        lateinit var host: FrameLayout
        var initialized = false
        val output = NativeVideoTestOutput(name)
        val minimumProgress = if (name == "black_h264.mp4") 200L else 1200L
        try {
            showEngine(context) { current, target ->
                engine = current
                host = target
                initialized = true
            }
            compose.runOnIdle {
                engine.open(file.toURI().toString(), software) { type, status ->
                    output.record(engine, type, status)
                }
            }
            compose.waitUntil(15_000L) { output.failed.get() || (output.videoOutput.get() && output.time.get() > minimumProgress) }
            assertTrue("Native decoder failed for $name", !output.failed.get())
            assertTrue("No advancing video output for $name", output.videoOutput.get() && output.time.get() > minimumProgress)
            assertTrue(
                "First output snapshot did not match $name: ${output.firstFrameDetails.get()}; " +
                    "position=${output.time.get()}ms, codec=${engine.metadata["video_format"]}",
                output.firstFrameValid.get(),
            )
            compose.runOnIdle {
                val poster = requireNotNull(engine.posterFrame)
                assertTrue("Poster exceeded its memory bound", poster.width <= 512 && poster.height <= 512)
                poster.recycle()
                if (checkDetached) assertOldTextureIgnored(engine, host, output.time.get(), output.rendered)
            }
            exercisePlayback(engine, output, name, name == "h264_aac.mp4")
        } finally {
            if (initialized) compose.runOnIdle { engine.close() }
            file.delete()
        }
    }

    private fun exercisePlayback(
        engine: MpvVideoEngine,
        output: NativeVideoTestOutput,
        name: String,
        checkSpeedChange: Boolean,
    ) {
        if (checkSpeedChange) {
            compose.runOnIdle { assertEquals(1500, engine.speed(1500)) }
            compose.waitUntil(15_000L) { output.failed.get() || output.time.get() > 2000L }
            assertTrue("AudioTrack stalled or failed after 1.5x", !output.failed.get() && output.time.get() > 2000L)
            compose.runOnIdle { assertEquals(1000, engine.speed(1000)) }
        }
        val duration = output.duration.get()
        val beforePosition = output.time.get()
        val candidates =
            listOf(duration / 3L, duration * 2L / 3L)
                .map { it.coerceIn(0L, (duration - SEEK_TOLERANCE_MS).coerceAtLeast(0L)) }
        val seekTarget =
            candidates
                .filter { beforePosition !in it..(it + SEEK_TOLERANCE_MS) }
                .maxByOrNull { abs(it - beforePosition) }
                ?: candidates.maxBy { abs(it - beforePosition) }
        compose.runOnIdle {
            engine.pause()
            engine.seekTo(seekTarget)
        }
        compose.waitUntil(15_000L) {
            output.failed.get() ||
                (
                    output.time.get() in seekTarget..(seekTarget + SEEK_TOLERANCE_MS)
                )
        }
        assertTrue(
            "Seek did not complete at $seekTarget ms for ${output.time.get()} ms",
            !output.failed.get() &&
                output.time.get() in seekTarget..(seekTarget + SEEK_TOLERANCE_MS),
        )
        val soughtPosition = output.time.get()
        compose.runOnIdle { engine.play() }
        compose.waitUntil(15_000L) {
            output.failed.get() || output.time.get() > soughtPosition + 100L
        }
        assertTrue(
            "Playback did not resume after seeking $name",
            !output.failed.get() && output.time.get() > soughtPosition + 100L,
        )
    }

    private fun showEngine(
        context: Context,
        onCreate: (MpvVideoEngine, FrameLayout) -> Unit,
    ) {
        lateinit var player: MpvVideoEngine
        compose.setContent {
            AndroidView(
                factory = {
                    player = MpvVideoEngine(context, OkHttpClient(), context.cacheDir)
                    FrameLayout(it).apply {
                        player.attach(this)
                        onCreate(player, this)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = { player.detach(it) },
            )
        }
    }

    private fun assertOldTextureIgnored(
        engine: MpvVideoEngine,
        host: FrameLayout,
        position: Long,
        rendered: AtomicInteger,
    ) {
        val texture = host.getChildAt(0) as TextureView
        val surface = requireNotNull(texture.surfaceTexture)
        val listener = requireNotNull(texture.surfaceTextureListener)
        engine.pause()
        engine.attach(FrameLayout(host.context))
        engine.seekTo(position)
        val before = rendered.get()
        listener.onSurfaceTextureUpdated(surface)
        assertEquals("Detached texture completed the replacement's callback", before, rendered.get())
        engine.attach(host)
        engine.play()
    }

    private companion object {
        const val SEEK_TOLERANCE_MS = 250L
    }
}
