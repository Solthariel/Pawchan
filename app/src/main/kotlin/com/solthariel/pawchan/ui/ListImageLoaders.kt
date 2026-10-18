package com.solthariel.pawchan.ui

import coil3.ImageLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

/** The two image paths used by catalog/thread lists, without viewer/download dependencies. */
internal data class ListImageLoaders(
    val imageLoader: ImageLoader,
    val thumbnailLoader: ThumbnailLoader,
)
