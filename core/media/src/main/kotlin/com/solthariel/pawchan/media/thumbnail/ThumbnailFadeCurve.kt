package com.solthariel.pawchan.media.thumbnail

import androidx.compose.animation.core.Easing

internal val ThumbnailFadeEasing =
    Easing { fraction ->
        val remaining = 1f - fraction.coerceIn(0f, 1f)
        1f - remaining * remaining
    }

internal const val THUMBNAIL_FADE_MILLIS = 200
