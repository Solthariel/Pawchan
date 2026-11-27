package com.solthariel.pawchan

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.video.MpvVideoEngine
import com.solthariel.pawchan.media.video.VideoFailure
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.ownedVideoSession
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Real Android/libmpv, deterministic HTTP responses. No contact with a server and no HTTP cache. */
class MpvHttpReplayRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val requests = AtomicInteger()
    private val unavailable = AtomicBoolean(false)

    @Test fun threeSecondHttpClipCanRepeatTenTimesOnOneGet() {
        withPlayer { player ->
            repeat(10) {
                await(player) { it.status.value.ended }
                compose.runOnIdle { player.replayFromEnd(automatic = true) }
                await(player) { it.status.value.playing && it.status.value.position > 0 && !it.status.value.ended }
            }
            await(player) { it.status.value.ended }
            assertEquals(1, requests.get())
        }
    }

    @Test fun httpFailureDoesNotSwitchDecoderAndOnlyAnExplicitRetryIssuesAnotherGet() {
        unavailable.set(true)
        withPlayer { player ->
            compose.waitUntil(10_000) { player.status.value.stage == VideoStage.ERROR }
            assertEquals(VideoFailure.SOURCE, player.status.value.attempt.failure)
            assertFalse(player.status.value.software)
            assertEquals(1, requests.get())
            unavailable.set(false)
            compose.runOnIdle { player.retry() }
            await(player) { it.hasVideoOutput.value && it.status.value.position > 0 }
            assertEquals(2, requests.get())
            assertFalse(player.status.value.software)
        }
    }

    private fun await(
        player: VideoPlaybackSession,
        condition: (VideoPlaybackSession) -> Boolean,
    ) {
        compose.waitUntil(20_000) { player.status.value.stage == VideoStage.ERROR || condition(player) }
        assertTrue("Playback failed: ${player.status.value}", player.status.value.stage != VideoStage.ERROR)
    }

    private fun withPlayer(test: (VideoPlaybackSession) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val bytes =
            instrumentation.context.assets
                .open("video/h264_aac.mp4")
                .use { it.readBytes() }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    requests.incrementAndGet()
                    Response
                        .Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(if (unavailable.get()) 503 else 200)
                        .message("fixture")
                        .body(bytes.toResponseBody())
                        .build()
                }.build()
        lateinit var host: FrameLayout
        var session: VideoPlaybackSession? = null
        try {
            compose.setContent {
                AndroidView(factory = { FrameLayout(it).also { view -> host = view } }, modifier = Modifier.fillMaxSize())
            }
            compose.runOnIdle {
                val backend = MpvVideoEngine(context, client, context.cacheDir)
                val player = ownedVideoSession(backend, "https://fixture.invalid/three-seconds.mp4")
                session = player
                backend.attach(host)
                backend.volume(0)
                player.start()
            }
            test(checkNotNull(session))
        } finally {
            compose.runOnIdle { session?.close() }
            client.connectionPool.evictAll()
        }
    }
}
