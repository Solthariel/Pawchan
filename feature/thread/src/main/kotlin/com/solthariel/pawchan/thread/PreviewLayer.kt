package com.solthariel.pawchan.thread

import androidx.compose.runtime.staticCompositionLocalOf

internal data class PreviewLayer(
    val depth: Int,
    val motion: PreviewSwipeState,
    val clear: () -> Unit,
)

internal val LocalPreviewLayer = staticCompositionLocalOf<PreviewLayer?> { null }
