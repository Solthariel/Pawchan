package com.solthariel.pawchan

import android.content.Context
import android.os.SystemClock
import android.util.Log
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Compatibility test of the actual debug mpv backend, not a release or smoothness benchmark. */
class MpvSyntheticFixturesRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private var host: FrameLayout? = null
    private val client = OkHttpClient()

    @Test fun syntheticClipsOpenSeekAndResumeInSwAndAutoHw() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val assets = instrumentation.context.assets
        val names =
            listOf(
                "synthetic-h264-1220x2712-fps24-17.mp4",
                "synthetic-mpeg4-704x416-fps37-457.mp4",
                "synthetic-vp8-vfr-640x360.webm",
                "synthetic-hevc-596x1280-fps42.mp4",
                "synthetic-h264-810x1440-fps60.mp4",
                "synthetic-h264-2160x3840-fps60.mp4",
                "synthetic-h264-2322x2160-fps60.mp4",
                "synthetic-h264-10bit-640x360-fps29-97.mp4",
            )
        compose.setContent {
            AndroidView(factory = { FrameLayout(it).also { view -> host = view } }, modifier = Modifier.fillMaxSize())
        }
        val failures = mutableListOf<String>()
        for (name in names) {
            val file = File.createTempFile("paw-synthetic-", ".video", context.cacheDir)
            try {
                assets.open("video/$name").use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                for (software in listOf(true, false)) {
                    val result = runCase(context, file, name, software)
                    if (result != null) failures.add(result)
                }
            } catch (error: Exception) {
                failures.add("$name: ${error.javaClass.simpleName}: ${error.message}")
                Log.e("PawMpvFixtures", "FIXTURE_ERROR file=$name type=${error.javaClass.simpleName}")
            } finally {
                file.delete()
            }
        }
        Log.i("PawMpvFixtures", "SUITE_END files=${names.size} cases=${names.size * 2} failures=${failures.size}")
        assertTrue("Fixture failures: ${failures.joinToString()}", failures.isEmpty())
    }

    private fun runCase(
        context: Context,
        file: File,
        name: String,
        software: Boolean,
    ): String? {
        val mode = if (software) "SW" else "AUTO_HW"
        val state = AtomicReference(VideoPlaybackStatus())
        val firstFrame = AtomicBoolean(false)
        val failed = AtomicBoolean(false)
        var engine: MpvVideoEngine? = null
        Log.i("PawMpvFixtures", "CASE_BEGIN file=$name requested=$mode")
        try {
            compose.runOnIdle {
                val player = MpvVideoEngine(context, client, context.cacheDir)
                engine = player
                player.attach(checkNotNull(host))
                player.muted(true)
                player.open(file.toURI().toString(), software) { event, status ->
                    state.set(status)
                    if (event == VideoEvent.VIDEO_OUTPUT) firstFrame.set(true)
                    if (event == VideoEvent.ERROR) failed.set(true)
                }
                player.pause()
            }
            requireState("open", failed, 30_000) { firstFrame.get() && state.get().seekable }
            compose.runOnIdle { checkNotNull(engine).seekTo(1500) }
            requireState("seek", failed, 30_000) { state.get().position in 1200..1800 }
            val afterSeek = state.get()
            check(!afterSeek.playing) { "playback resumed while paused" }
            compose.runOnIdle { checkNotNull(engine).play() }
            requireState("resume", failed, 30_000) { state.get().position >= 2000 || state.get().ended }
            val result = state.get()
            Log.i(
                "PawMpvFixtures",
                "CASE_END file=$name requested=$mode ok=true actual_hw=${result.hardwareDecoded} position_ms=${result.position} ended=${result.ended}",
            )
            return null
        } catch (error: Exception) {
            Log.e(
                "PawMpvFixtures",
                "CASE_END file=$name requested=$mode ok=false type=${error.javaClass.simpleName} message=${error.message}",
            )
            return "$name ($mode): ${error.message}"
        } finally {
            compose.runOnIdle { engine?.close() }
            SystemClock.sleep(1_500)
        }
    }

    private fun requireState(
        stage: String,
        failed: AtomicBoolean,
        timeoutMs: Long,
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (!failed.get() && !condition() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
        check(!failed.get()) { "$stage: mpv reported an error" }
        check(condition()) { "$stage: timeout after ${timeoutMs}ms" }
    }
}
