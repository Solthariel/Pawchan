package com.solthariel.pawchan.composer

import android.graphics.Bitmap

internal data class ImageCanvasOptions(
    val enabled: Boolean = true,
    val comparisonBitmap: Bitmap? = null,
)
