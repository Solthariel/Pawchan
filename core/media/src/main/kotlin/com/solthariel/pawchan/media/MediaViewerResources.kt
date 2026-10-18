package com.solthariel.pawchan.media

import coil3.ImageLoader
import okhttp3.OkHttpClient

/** Borrowed resources: the app creator owns and closes them, not a viewer composition. */
data class MediaViewerResources(
    val imageLoader: ImageLoader,
    val contentClient: OkHttpClient,
)
