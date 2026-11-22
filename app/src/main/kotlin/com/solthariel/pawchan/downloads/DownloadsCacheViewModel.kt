package com.solthariel.pawchan.downloads

import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.ViewModel

/** Keeps only bounded preview bitmaps while the navigation host is alive. */
internal class DownloadsCacheViewModel : ViewModel() {
    val thumbnails = LruCache<String, Bitmap>(THUMBNAIL_CACHE_SIZE)

    override fun onCleared() {
        thumbnails.evictAll()
    }
}

private const val THUMBNAIL_CACHE_SIZE = 32
