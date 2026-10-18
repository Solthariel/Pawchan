package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.solthariel.pawchan.media.MediaLoadingImage
import com.solthariel.pawchan.media.thumbnail.LocalThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.video.LocalVideoPosterStore
import com.solthariel.pawchan.media.video.VideoPosterFallback
import com.solthariel.pawchan.media.video.VideoPosterStore
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class VideoPosterFallbackRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun missingThumbnailUsesReservedFrame() {
        val fixture = PosterFailureFixture()
        try {
            compose.runOnIdle { fixture.store.capture(fixture.player) }
            show(fixture, null)
            assertRedReserve()
            assertTrue(fixture.events.contains("missing_url"))
        } finally {
            compose.runOnIdle { fixture.close() }
        }
    }

    @Test
    fun firstVideoWithoutThumbnailHasNonBlackPlaceholder() {
        val fixture = PosterFailureFixture()
        try {
            show(fixture, null)
            compose.onNodeWithContentDescription("Загрузка видео").assertIsDisplayed()
            val pixels = compose.onNodeWithTag("poster").captureToImage().toPixelMap()
            val corner = pixels[pixels.width / 4, pixels.height / 4]
            assertTrue(corner.red + corner.green + corner.blue > 0f)
        } finally {
            compose.runOnIdle { fixture.close() }
        }
    }

    @Test
    fun failedThumbnailAndFailedCoilKeepReservedFrameVisible() {
        val fixture = PosterFailureFixture()
        try {
            compose.runOnIdle { fixture.store.capture(fixture.player) }
            show(fixture, "https://poster.example/failed.png")
            compose.waitUntil(10_000L) {
                fixture.events.contains("thumbnail_failed") && fixture.events.contains("coil_failed")
            }
            assertRedReserve()
        } finally {
            compose.runOnIdle { fixture.close() }
        }
    }

    private fun show(
        fixture: PosterFailureFixture,
        url: String?,
    ) {
        compose.setContent {
            CompositionLocalProvider(
                LocalVideoPosterStore provides fixture.store,
                LocalThumbnailLoader provides fixture.thumbnails,
            ) {
                MediaLoadingImage(
                    url,
                    fixture.loader,
                    Modifier.testTag("poster"),
                    onState = { fixture.events.add(it) },
                    fallback = { VideoPosterFallback() },
                )
            }
        }
    }

    private fun assertRedReserve() {
        compose.onNodeWithContentDescription("Загрузка видео").assertIsDisplayed()
        val pixels = compose.onNodeWithTag("poster").captureToImage().toPixelMap()
        val center = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(center.red > 0.9f && center.green < 0.1f && center.blue < 0.1f)
    }
}

private class PosterFailureFixture : AutoCloseable {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val bitmap =
        Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
    val player = posterTestSession(true, PosterTestEngine(bitmap))
    val store = VideoPosterStore()
    val events = ConcurrentLinkedQueue<String>()
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Missing thumbnail")
                    .body("missing".toResponseBody())
                    .build()
            }.build()
    val thumbnails = ThumbnailLoader(context, client)
    val loader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()

    override fun close() {
        player.close()
        store.clear()
        thumbnails.close()
        loader.shutdown()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
