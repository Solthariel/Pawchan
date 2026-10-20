package com.solthariel.pawchan.media

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.domain.model.MediaKind
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ViewerImageFailureRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val failOriginal = AtomicBoolean(true)
    private val failThumbnail = AtomicBoolean(false)
    private val completedOriginals = AtomicInteger()
    private var image by mutableStateOf(attachment("original", "thumbnail"))
    private var thumbnailGate: CompletableDeferred<Unit>? = null
    private val loader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    Interceptor { chain ->
                        val original =
                            chain.request.data
                                .toString()
                                .contains("original")
                        if (!original) thumbnailGate?.await()
                        val failed = if (original) failOriginal.get() else failThumbnail.get()
                        val result =
                            if (failed) {
                                ErrorResult(
                                    image = null,
                                    request = chain.request,
                                    throwable = IOException("HTTP 404 fixture"),
                                )
                            } else {
                                val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(if (original) Color.BLUE else Color.RED)
                                SuccessResult(bitmap.asImage(), chain.request, DataSource.NETWORK)
                            }
                        if (original) completedOriginals.incrementAndGet()
                        result
                    },
                )
            }.build()

    @After fun close() = loader.shutdown()

    @Test fun originalFailureKeepsTheAvailableThumbnailWithoutAnErrorLabel() {
        show()
        compose.waitUntil(timeoutMillis = 5000) { completedOriginals.get() > 0 }
        assertImage(red = true)
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertDoesNotExist()
    }

    @Test fun failureWithoutAnyImageKeepsTheExistingErrorAndRetry() {
        failThumbnail.set(true)
        show()
        compose.waitUntil(timeoutMillis = 5000) { hasError() }
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.retry)).assertIsDisplayed()
    }

    @Test fun failedRefreshKeepsThePreviouslyLoadedOriginal() {
        failOriginal.set(false)
        show()
        assertImage(red = false)
        failOriginal.set(true)
        compose.onNodeWithContentDescription(context.getString(R.string.media_refresh)).performClick()
        compose.waitUntil(timeoutMillis = 5000) { completedOriginals.get() >= 2 }
        assertImage(red = false)
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertDoesNotExist()
    }

    @Test fun movingToAnotherUrlDoesNotReuseThePreviousImage() {
        failOriginal.set(false)
        show()
        assertImage(red = false)
        failOriginal.set(true)
        compose.runOnIdle { image = attachment("original-2", null) }
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 5000) { hasError() }
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertIsDisplayed()
    }

    @Test fun aLateThumbnailCanStillAppearAfterTheOriginalHasFailed() {
        thumbnailGate = CompletableDeferred()
        show()
        compose.waitUntil(timeoutMillis = 5000) { completedOriginals.get() > 0 }
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertDoesNotExist()
        thumbnailGate?.complete(Unit)
        assertImage(red = true)
        compose.onNodeWithText(context.getString(R.string.image_load_failed)).assertDoesNotExist()
    }

    private fun show() {
        compose.setContent { MaterialTheme { ImageViewer(image, loader, {}, null, {}) } }
    }

    private fun assertImage(red: Boolean) {
        compose.waitUntil(timeoutMillis = 5000) {
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val pixel = pixels[pixels.width / 2, pixels.height / 2]
            if (red) pixel.red > 0.9f && pixel.blue < 0.1f else pixel.blue > 0.9f && pixel.red < 0.1f
        }
        assertTrue(completedOriginals.get() > 0)
    }

    private fun hasError(): Boolean =
        compose.onAllNodesWithText(context.getString(R.string.image_load_failed)).fetchSemanticsNodes().isNotEmpty()

    private fun attachment(
        original: String,
        thumbnail: String?,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(
                original,
                "image.jpg",
                "https://fixture.test/$original",
                thumbnail?.let { "https://fixture.test/$it" },
            ),
            null,
            AttachmentViewer.IMAGE,
            MediaKind.IMAGE,
        )
}
