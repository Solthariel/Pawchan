package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.solthariel.pawchan.media.AttachmentImagePreloader
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Real Coil disk cache and Android decoding with an in-memory HTTP response fixture. */
class AttachmentImagePreloaderRuntimeTest {
    @Test
    fun completedPreloadIsReusedByViewerAndDoesNotPopulateMemory() =
        runBlocking {
            AttachmentPreloadFixture().use { fixture ->
                val image = fixture.attachment("image")
                fixture.preloader.preload(listOf(image, image))
                assertEquals(1, fixture.requests.get())
                assertEquals(0L, fixture.loader.memoryCache?.size ?: 0L)
                fixture.preloader.preload(listOf(image))
                assertEquals(1, fixture.requests.get())
                assertTrue(fixture.open(image) is SuccessResult)
                assertEquals(1, fixture.requests.get())
            }
        }

    @Test
    fun videosNeverStartAnOriginalRequest() =
        runBlocking {
            AttachmentPreloadFixture().use { fixture ->
                val video = fixture.attachment("video").copy(viewer = AttachmentViewer.VIDEO)
                fixture.preloader.preload(listOf(video))
                assertEquals(0, fixture.requests.get())
            }
        }

    @Test
    fun failedPreloadDoesNotRetryOrBlockNextImageAndViewerCanRetry() =
        runBlocking {
            AttachmentPreloadFixture(failFirst = true).use { fixture ->
                val first = fixture.attachment("first")
                val second = fixture.attachment("second")
                fixture.preloader.preload(listOf(first, second))
                assertEquals(2, fixture.requests.get())
                assertTrue(fixture.open(second) is SuccessResult)
                assertEquals(2, fixture.requests.get())
                assertTrue(fixture.open(first) is SuccessResult)
                assertEquals(3, fixture.requests.get())
            }
        }
}

private class AttachmentPreloadFixture(
    failFirst: Boolean = false,
) : AutoCloseable {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = context.cacheDir.resolve("attachment-preload-test-${UUID.randomUUID()}")
    private val bytes = preloadPng()
    val requests = AtomicInteger()
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val ordinal = requests.incrementAndGet()
                val fail = failFirst && ordinal == 1
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (fail) 500 else 200)
                    .message(if (fail) "Fixture failure" else "OK")
                    .header("Content-Type", "image/png")
                    .header("Cache-Control", "max-age=86400")
                    .body(bytes.toResponseBody())
                    .build()
            }.build()
    val loader =
        ImageLoader
            .Builder(context)
            .diskCache {
                DiskCache
                    .Builder()
                    .directory(directory)
                    .maxSizeBytes(4L * 1024 * 1024)
                    .build()
            }.components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    val preloader = AttachmentImagePreloader(context, loader)

    fun attachment(id: String): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.png", "https://preload.example/$id.png", null),
            bytes.size.toLong(),
            AttachmentViewer.IMAGE,
        )

    suspend fun open(image: MediaAttachmentUi): coil3.request.ImageResult =
        loader.execute(
            ImageRequest
                .Builder(context)
                .data(image.originalUrl)
                .diskCacheKey(image.originalUrl)
                .memoryCacheKey("${image.originalUrl}:0")
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
        )

    override fun close() {
        loader.shutdown()
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        directory.deleteRecursively()
    }
}

private fun preloadPng(): ByteArray {
    val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
    return try {
        ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        }
    } finally {
        bitmap.recycle()
    }
}
