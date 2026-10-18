package com.solthariel.pawchan.media

import android.content.Context
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BlackholeDecoder
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Warms the viewer's disk cache without allocating full-size bitmaps or GIF frames. */
internal class AttachmentImagePreloader(
    private val context: Context,
    private val loader: ImageLoader,
) {
    suspend fun preload(attachments: List<MediaAttachmentUi>) {
        preloadAttachmentImages(attachments, ::load)
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun load(url: String) {
        withContext(Dispatchers.IO) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .diskCacheKey(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .decoderFactory(BlackholeDecoder.Factory())
                    .build()
            // Coil returns ErrorResult for normal failures; continue without retrying.
            // CancellationException propagates from execute to the picker's lifecycle owner.
            loader.execute(request)
        }
    }
}
