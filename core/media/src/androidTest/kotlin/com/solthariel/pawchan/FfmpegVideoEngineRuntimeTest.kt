package com.solthariel.pawchan

import android.content.Context
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.FfmpegVideoEngine
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FfmpegVideoEngineRuntimeTest {
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
        val file = File.createTempFile("ffmpeg-fixture-", ".media", context.cacheDir)
        instrumentation.context.assets.open("video/$name").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        lateinit var engine: FfmpegVideoEngine
        lateinit var host: FrameLayout
        var initialized = false
        val output = NativeVideoTestOutput(name)
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
            compose.waitUntil(15_000L) { output.failed.get() || (output.videoOutput.get() && output.time.get() > 200L) }
            assertTrue("Native decoder failed for $name", !output.failed.get())
            assertTrue("No advancing video output for $name", output.videoOutput.get() && output.time.get() > 200L)
            assertTrue("First output snapshot did not match $name", output.firstFrameValid.get())
            compose.runOnIdle {
                val poster = requireNotNull(engine.posterFrame)
                assertTrue("Poster exceeded its memory bound", poster.width <= 512 && poster.height <= 512)
                poster.recycle()
                if (checkDetached) assertOldTextureIgnored(engine, host, output.time.get(), output.rendered)
            }
            val seekTarget = (output.duration.get() / 3L).coerceAtMost(1000L)
            val beforeSeek = output.rendered.get()
            compose.runOnIdle {
                engine.pause()
                engine.seekTo(seekTarget)
                engine.play()
            }
            compose.waitUntil(15_000L) {
                output.failed.get() || (output.rendered.get() > beforeSeek && output.time.get() > seekTarget + 100L)
            }
            assertTrue("Seek failed for $name", !output.failed.get() && output.rendered.get() > beforeSeek)
        } finally {
            if (initialized) compose.runOnIdle { engine.close() }
            file.delete()
        }
    }

    private fun showEngine(
        context: Context,
        onCreate: (FfmpegVideoEngine, FrameLayout) -> Unit,
    ) {
        lateinit var player: FfmpegVideoEngine
        compose.setContent {
            AndroidView(
                factory = {
                    player = FfmpegVideoEngine(context, OkHttpClient(), context.cacheDir)
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
        engine: FfmpegVideoEngine,
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
}
