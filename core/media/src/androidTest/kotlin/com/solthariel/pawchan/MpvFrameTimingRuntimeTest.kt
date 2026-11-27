package com.solthariel.pawchan

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvFrameCounters
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoEvent
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/** Compatibility tests seek early; this probe plays each 60 fps clip from start to natural EOF instead. */
class MpvFrameTimingRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fullHd60() = measure("fhd60", "synthetic-h264-1080x1920-fps60.mp4")

    @Test fun vertical4k60() = measure("v4k60", "synthetic-h264-2160x3840-fps60.mp4")

    private fun measure(
        name: String,
        asset: String,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File.createTempFile("frame-timing-", ".mp4", instrumentation.targetContext.cacheDir)
        val run = InstrumentationRegistry.getArguments().getString("pawTimingRun") ?: "device-smoke"
        lateinit var host: FrameLayout
        try {
            instrumentation.context.assets.open("video/$asset").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            compose.setContent {
                AndroidView(factory = { FrameLayout(it).also { box -> host = box } }, modifier = Modifier.fillMaxSize())
            }
            compose.waitForIdle()
            playOne(file, host) // Same local clip and surface setup for a single unmeasured warm-up.
            repeat(MEASURED_TRIALS) { trial ->
                val result = playOne(file, host)
                Log.i("PawMpvFixtures", "PAW_FRAME run=$run case=$name trial=${trial + 1} $result")
            }
        } finally {
            file.delete()
        }
    }

    private fun playOne(
        file: File,
        host: FrameLayout,
    ): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val window = CounterWindow()
        val firstFrame = AtomicLong(-1L)
        val ended = AtomicBoolean()
        val failed = AtomicBoolean()
        var engine: MpvVideoEngine? = null
        try {
            compose.runOnIdle {
                val player = MpvVideoEngine(context, null, context.cacheDir, window::sample)
                engine = player
                player.attach(host)
                player.volume(0)
                val start = SystemClock.elapsedRealtime()
                player.open(file.toURI().toString(), software = false) { event, _ ->
                    if (event == VideoEvent.VIDEO_OUTPUT) firstFrame.compareAndSet(-1L, SystemClock.elapsedRealtime() - start)
                    if (event == VideoEvent.ERROR) failed.set(true)
                    if (event == VideoEvent.ENDED) ended.set(true)
                }
            }
            compose.waitUntil(60_000L) { ended.get() || failed.get() }
            check(!failed.get() && ended.get() && firstFrame.get() >= 0) { "No completed playback with a real first frame" }
            return "first_ms=${firstFrame.get()} ${window.result()} " +
                "model=${Build.MODEL.replace(' ', '_')} api=${Build.VERSION.SDK_INT}"
        } finally {
            compose.runOnIdle { engine?.close() }
        }
    }
}

private class CounterWindow {
    private val first = AtomicReference<MpvFrameCounters?>()
    private val last = AtomicReference<MpvFrameCounters?>()

    fun sample(next: MpvFrameCounters) {
        if (next.position <= 0L) return
        first.compareAndSet(null, next)
        if (next.position >= (last.get()?.position ?: 0L)) last.set(next)
    }

    fun result(): String {
        val a = checkNotNull(first.get())
        val b = checkNotNull(last.get())
        check(b.position > a.position) { "Insufficient counter sample window" }
        val decoder = b.hardware?.let { if (it) "hw" else "sw" } ?: "unknown"
        return "from_ms=${a.position} to_ms=${b.position} decoder=$decoder " +
            "decoder_drops=${delta(a.decoderDrops, b.decoderDrops)} output_drops=${delta(a.outputDrops, b.outputDrops)}"
    }

    private fun delta(
        first: Long?,
        last: Long?,
    ): String = if (first == null || last == null || last < first) "NA" else (last - first).toString()
}

private const val MEASURED_TRIALS = 5
