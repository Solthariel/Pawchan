package com.solthariel.pawchan.thread

import androidx.compose.runtime.staticCompositionLocalOf

internal data class PreviewLayer(
    val depth: Int,
    val motion: PreviewSwipeState,
    val clear: () -> Unit,
    val count: Int = 1,
)

internal val LocalPreviewLayer = staticCompositionLocalOf<PreviewLayer?> { null }
