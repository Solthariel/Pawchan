package com.solthariel.pawchan.thread

import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

internal data class PostPreviewPresentation(
    val appearance: ThreadAppearance,
    val thumbnails: ThumbnailLoader,
)
