package com.solthariel.pawchan

import android.graphics.Bitmap
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.LocalMediaChromeState
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaChromeState
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.MediaViewerWindow
import com.solthariel.pawchan.media.video.LocalVideoPosterStore
import com.solthariel.pawchan.media.video.VideoEngineBackend
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoPosterStore
import com.solthariel.pawchan.media.video.VideoSurface
import com.solthariel.pawchan.media.video.VideoSurfaceActions
import com.solthariel.pawchan.media.video.ownedVideoSession
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import android.graphics.Color as AndroidColor

class VideoViewerBackdropRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun waitingAndSurfaceResetShowOnlyNinetyPercentDimEvenWithAReservedFrame() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val previous = posterTestSession(true, PosterTestEngine(bitmap))
        val store = VideoPosterStore()
        val player = ownedVideoSession(SolidFrameEngine(android.graphics.Color.RED), "video")
        val loader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
        try {
            compose.runOnIdle { store.capture(previous) }
            showSurface(player, store, loader)
            assertCenter(0.1f, 0.1f, 0.1f)
            compose.runOnIdle { player.mutableVideoOutput.value = true }
            assertCenter(1f, 0f, 0f)
            compose.runOnIdle { player.mutableVideoOutput.value = false }
            assertCenter(0.1f, 0.1f, 0.1f)
        } finally {
            compose.runOnIdle {
                player.close()
                previous.close()
                store.clear()
            }
            loader.shutdown()
            bitmap.recycle()
        }
    }

    @Test
    fun blackVideoFrameIsVisibleWhenOutputIsReady() {
        val player = ownedVideoSession(SolidFrameEngine(android.graphics.Color.BLACK), "video")
        val loader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
        try {
            showSurface(player, VideoPosterStore(), loader)
            assertCenter(0.1f, 0.1f, 0.1f)
            compose.runOnIdle { player.mutableVideoOutput.value = true }
            assertCenter(0f, 0f, 0f)
        } finally {
            compose.runOnIdle { player.close() }
            loader.shutdown()
        }
    }

    @Test
    fun actualViewerWindowShowsTheDimmedPageWhileVideoIsWaiting() {
        val player = ownedVideoSession(SolidFrameEngine(android.graphics.Color.RED), "video")
        val loader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
        try {
            compose.setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize().background(Color.White))
                    MediaViewerWindow(onClose = {}) {
                        SurfaceFixture(player, VideoPosterStore(), loader)
                    }
                }
            }
            compose.waitForIdle()
            compose.waitUntil(5_000) { windowShowsDimmedWhitePage() }
        } finally {
            compose.runOnIdle { player.close() }
            loader.shutdown()
        }
    }

    @Test
    fun playingLandscapeFrameHasDimmedBackdropOutsideItsBounds() {
        val player = ownedVideoSession(SolidFrameEngine(android.graphics.Color.RED), "video")
        val loader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
        try {
            compose.setContent {
                val chrome = MediaChromeState().apply { visible = false }
                MaterialTheme {
                    Box(Modifier.size(240.dp).background(Color.White).testTag("fitted-stage")) {
                        CompositionLocalProvider(LocalMediaChromeState provides chrome) {
                            SurfaceFixture(player, VideoPosterStore(), loader, MediaDimensions.create(1920, 1080))
                        }
                    }
                }
            }
            compose.runOnIdle { player.mutableVideoOutput.value = true }
            val pixels = compose.onNodeWithTag("fitted-stage").captureToImage().toPixelMap()
            val center = pixels[pixels.width / 2, pixels.height / 2]
            val outside = pixels[pixels.width / 2, pixels.height / 10]
            assertEquals(1f, center.red, 0.03f)
            assertEquals(0f, center.green, 0.03f)
            assertEquals(0.1f, outside.red, 0.03f)
            assertEquals(0.1f, outside.green, 0.03f)
            assertEquals(0.1f, outside.blue, 0.03f)
        } finally {
            compose.runOnIdle { player.close() }
            loader.shutdown()
        }
    }

    private fun windowShowsDimmedWhitePage(): Boolean {
        val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return false
        return try {
            val color = screenshot.getPixel(screenshot.width / 2, screenshot.height / 2)
            intArrayOf(AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color)).all {
                kotlin.math.abs(it / 255f - 0.1f) <= 0.03f
            }
        } finally {
            screenshot.recycle()
        }
    }

    private fun showSurface(
        player: VideoPlaybackSession,
        store: VideoPosterStore,
        loader: ImageLoader,
    ) {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(Color.White).testTag("video-stage")) {
                    SurfaceFixture(player, store, loader)
                }
            }
        }
    }

    @Composable
    private fun SurfaceFixture(
        player: VideoPlaybackSession,
        store: VideoPosterStore,
        loader: ImageLoader,
        dimensions: MediaDimensions? = null,
    ) {
        val video =
            MediaAttachmentUi(
                source = MediaAttachmentSource("video", "video.mp4", "https://example.test/video.mp4", null),
                byteSize = null,
                viewer = AttachmentViewer.VIDEO,
                dimensions = dimensions,
            )
        CompositionLocalProvider(LocalVideoPosterStore provides store) {
            VideoSurface(
                player = player,
                video = video,
                state = VideoPlaybackStatus(),
                actions = VideoSurfaceActions(loader, {}, {}, null, {}, null),
                navigation = MediaViewerNavigation(),
            )
        }
    }

    private fun assertCenter(
        red: Float,
        green: Float,
        blue: Float,
    ) {
        val pixels = compose.onNodeWithTag("video-stage").captureToImage().toPixelMap()
        val color = pixels[pixels.width / 2, pixels.height / 2]
        assertEquals(red, color.red, 0.03f)
        assertEquals(green, color.green, 0.03f)
        assertEquals(blue, color.blue, 0.03f)
    }
}

private class SolidFrameEngine(
    private val color: Int,
) : VideoEngineBackend {
    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) = Unit

    override fun attach(view: FrameLayout) = view.setBackgroundColor(color)

    override fun detach(view: FrameLayout) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(position: Long) = Unit

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int): Int = milli

    override fun stop() = Unit

    override fun close() = Unit
}
