package com.solthariel.pawchan

import androidx.compose.ui.test.junit4.v2.createComposeRule
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.solthariel.pawchan.media.rememberMediaImageRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaImageRequestRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun currentNeighborAndRetryShareCacheKeysButRetryBypassesReads() {
        lateinit var current: ImageRequest
        lateinit var neighbor: ImageRequest
        lateinit var retry: ImageRequest
        val url = "https://example.test/image.png"
        compose.setContent {
            current = rememberMediaImageRequest(url)
            neighbor = rememberMediaImageRequest(url)
            retry = rememberMediaImageRequest(url, 1)
        }
        compose.runOnIdle {
            assertEquals(current.memoryCacheKey, neighbor.memoryCacheKey)
            assertEquals(current.memoryCacheKey, retry.memoryCacheKey)
            assertEquals(url, current.diskCacheKey)
            assertEquals(current.diskCacheKey, retry.diskCacheKey)
            assertEquals(CachePolicy.ENABLED, current.memoryCachePolicy)
            assertEquals(CachePolicy.WRITE_ONLY, retry.memoryCachePolicy)
            assertEquals(CachePolicy.WRITE_ONLY, retry.diskCachePolicy)
        }
    }
}
