package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.media.thumbnail.ThumbnailDiskCache
import com.solthariel.pawchan.media.thumbnail.ThumbnailPipeline
import com.solthariel.pawchan.media.thumbnail.ThumbnailTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.UUID

class ThumbnailCacheAdmissionRuntimeTest {
    @Test
    fun diskHitLoadsWithoutAnyNetworkStartGate() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val directory = context.cacheDir.resolve("thumbnail-fixture-${UUID.randomUUID()}")
            val disk = ThumbnailDiskCache(directory, 1_024L * 1_024L)
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor {
                        error("Disk hit must not reach HTTP")
                    }.build()
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                withContext(Dispatchers.IO) { disk.store("disk-hit", bitmap) }
                val pipeline = ThumbnailPipeline(client, disk, Dispatchers.IO, context.resources.displayMetrics.density)
                // A miss would fail because this task deliberately has no gate.
                val task = ThumbnailTask("disk-hit", "https://example.invalid/thumbnail.png")
                val result = withTimeout(5_000) { pipeline.load(task) }
                assertNotNull(result.bitmap)
            } finally {
                bitmap.recycle()
                disk.close()
                withContext(Dispatchers.IO) { directory.deleteRecursively() }
                client.dispatcher.executorService.shutdownNow()
                client.connectionPool.evictAll()
            }
        }
}
