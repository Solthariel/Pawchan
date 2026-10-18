package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailMemoryCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** In-memory HTTP fixtures, real Android bitmap/cache operations; no external network. */
class ThumbnailLoaderRuntimeTest {
    @Test
    fun duplicateTargetsUseOneRequestAndLaterAttachHitsMemory() =
        runBlocking {
            val requests = AtomicInteger()
            val client = imageClient(requests)
            val loader = loader(client)
            val url = uniqueUrl()
            try {
                val targets =
                    withContext(Dispatchers.Main) {
                        listOf(loader.subscription(url), loader.subscription(url)).also {
                            it.forEach { target -> target.onRemembered() }
                        }
                    }
                val first =
                    withTimeout(5_000) {
                        targets
                            .first()
                            .bitmap
                            .filterNotNull()
                            .first()
                    }
                val second =
                    withTimeout(5_000) {
                        targets
                            .last()
                            .bitmap
                            .filterNotNull()
                            .first()
                    }
                assertSame(first, second)
                assertEquals(1, requests.get())
                withContext(Dispatchers.Main) {
                    targets.forEach { it.onForgotten() }
                    val memoryTarget = loader.subscription(url)
                    memoryTarget.onRemembered()
                    assertSame(first, memoryTarget.bitmap.value)
                    assertTrue(memoryTarget.instantly)
                    memoryTarget.onForgotten()
                }
                assertEquals(1, requests.get())
            } finally {
                withContext(Dispatchers.Main) { loader.close() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }

    @Test
    fun forgettingTargetBeforeItsDwellStartsNoNetworkRequest() =
        runBlocking {
            val requests = AtomicInteger()
            val client = imageClient(requests)
            val loader = loader(client)
            try {
                withContext(Dispatchers.Main) {
                    val target = loader.subscription(uniqueUrl())
                    target.onRemembered()
                    target.onForgotten()
                }
                delay(750)
                assertEquals(0, requests.get())
            } finally {
                withContext(Dispatchers.Main) { loader.close() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }

    @Test
    fun laterSubscriberKeepsTheExistingDwellAndSurvivesFirstSubscriberDisposal() =
        runBlocking {
            val requests = AtomicInteger()
            val client = imageClient(requests)
            val loader = loader(client)
            val url = uniqueUrl()
            try {
                val first =
                    withContext(Dispatchers.Main) {
                        loader.subscription(url).also { it.onRemembered() }
                    }
                delay(250)
                val second =
                    withContext(Dispatchers.Main) {
                        loader.subscription(url).also {
                            it.onRemembered()
                            assertSame(first.task, it.task)
                            assertSame(first.task?.startGate, it.task?.startGate)
                            first.onForgotten()
                        }
                    }
                withTimeout(5_000) { second.bitmap.filterNotNull().first() }
                assertEquals(1, requests.get())
                withContext(Dispatchers.Main) { second.onForgotten() }
            } finally {
                withContext(Dispatchers.Main) { loader.close() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }

    @Test
    fun memoryCacheEvictsLeastRecentlyUsedWithoutRecyclingDisplayedBitmap() {
        val cache = ThumbnailMemoryCache(maxEntries = 2, maxSizeBytes = 1_024)
        val first = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val second = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val third = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            cache.put("first", first)
            cache.put("second", second)
            assertSame(first, cache["first"])
            cache.put("third", third)
            assertEquals(null, cache["second"])
            assertEquals(false, second.isRecycled)
        } finally {
            cache.clear()
            first.recycle()
            second.recycle()
            third.recycle()
        }
    }

    @Test
    fun cancellationOfLastTargetCancelsTheActiveHttpCall() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Boolean>()
            val release = CountDownLatch(1)
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        started.complete(Unit)
                        release.await(5, TimeUnit.SECONDS)
                        cancelled.complete(chain.call().isCanceled())
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(503)
                            .message("test")
                            .body("".toResponseBody())
                            .build()
                    }.build()
            val loader = loader(client)
            try {
                val target =
                    withContext(Dispatchers.Main) {
                        loader.subscription(uniqueUrl()).also { it.onRemembered() }
                    }
                withTimeout(5_000) { started.await() }
                withContext(Dispatchers.Main) { target.onForgotten() }
                release.countDown()
                assertTrue(withTimeout(5_000) { cancelled.await() })
            } finally {
                release.countDown()
                withContext(Dispatchers.Main) { loader.close() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }

    @Test
    fun failedUrlIsNegativelyCachedAndManualRefreshRetriesActiveTarget() =
        runBlocking {
            val requests = AtomicInteger()
            val client = imageClient(requests, failFirst = true)
            val loader = loader(client)
            val url = uniqueUrl()
            try {
                val first =
                    withContext(Dispatchers.Main) {
                        loader.subscription(url).also { it.onRemembered() }
                    }
                withTimeout(5_000) { first.failed.first { it } }
                val second =
                    withContext(Dispatchers.Main) {
                        first.onForgotten()
                        loader.subscription(url).also { it.onRemembered() }
                    }
                assertEquals(1, requests.get())
                assertTrue(second.failed.value)
                withContext(Dispatchers.Main) { loader.retryVisible() }
                withTimeout(5_000) { second.bitmap.filterNotNull().first() }
                assertEquals(2, requests.get())
                withContext(Dispatchers.Main) { second.onForgotten() }
            } finally {
                withContext(Dispatchers.Main) { loader.close() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }

    private fun loader(client: OkHttpClient): ThumbnailLoader =
        ThumbnailLoader(InstrumentationRegistry.getInstrumentation().targetContext, client)

    private fun uniqueUrl(): String = "https://example.invalid/${UUID.randomUUID()}.png"

    private fun imageClient(
        requests: AtomicInteger,
        failFirst: Boolean = false,
    ): OkHttpClient {
        val bytes =
            ByteArrayOutputStream().use { output ->
                val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                bitmap.recycle()
                output.toByteArray()
            }
        return OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val status = if (requests.incrementAndGet() == 1 && failFirst) 503 else 200
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("test")
                    .body(bytes.toResponseBody())
                    .build()
            }.build()
    }
}
