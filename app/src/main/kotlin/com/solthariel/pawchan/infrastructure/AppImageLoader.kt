package com.solthariel.pawchan.infrastructure

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.solthariel.pawchan.media.MediaImageTiming
import okhttp3.OkHttpClient

internal fun createAppImageLoader(
    context: Context,
    client: OkHttpClient,
    bytes: Long,
): ImageLoader =
    ImageLoader
        .Builder(context.applicationContext)
        .eventListenerFactory { MediaImageTiming.create() }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context.applicationContext, IMAGE_MEMORY_PERCENT).build() }
        .diskCache {
            DiskCache
                .Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(bytes)
                .build()
        }.components {
            add(OkHttpNetworkFetcherFactory(callFactory = { client }))
            add(AnimatedImageDecoder.Factory())
        }.build()

private const val IMAGE_MEMORY_PERCENT = 0.25
