package com.solthariel.pawchan

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.FfmpegVideoEngine
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File

class VideoSessionReliabilityRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lastSeekDuringNativeOpeningIsAppliedWhilePaused() = checkOpening(switchDecoder = false)

    @Test
    fun replacingOpeningDecoderRetainsPendingPosition() = checkOpening(switchDecoder = true)

    private fun checkOpening(switchDecoder: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File.createTempFile("session-reliability-", ".mp4", context.cacheDir)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var session: VideoPlaybackSession? = null
        var host: FrameLayout? = null
        try {
            instrumentation.context.assets.open("video/h264_aac.mp4").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            compose.setContent {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext -> FrameLayout(viewContext).also { host = it } },
                )
            }
            compose.runOnIdle {
                val engine = FfmpegVideoEngine(context, OkHttpClient(), context.cacheDir)
                val player = VideoPlaybackSession(engine, scope, file.toURI().toString())
                session = player
                engine.attach(checkNotNull(host))
                player.startAt(0, playing = false)
                player.seekTo(200)
                player.seekTo(600)
                if (switchDecoder) player.setSoftware(true)
            }
            compose.waitUntil(timeoutMillis = 20_000) {
                val status = session?.status?.value
                status?.stage == VideoStage.ERROR ||
                    (status?.stage == VideoStage.READY && status.position in 500L..850L)
            }
            compose.runOnIdle {
                val player = checkNotNull(session)
                assertEquals(VideoStage.READY, player.status.value.stage)
                assertFalse(player.status.value.playing)
                if (switchDecoder) assertEquals(true, player.status.value.software)
            }
        } finally {
            compose.runOnIdle {
                host?.let { session?.engine?.detach(it) }
                session?.close()
                session?.close()
                scope.cancel()
            }
            file.delete()
        }
    }
}
