package com.solthariel.pawchan.settings

import androidx.compose.material3.ColorScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

internal data class ColorPreviewRequest(
    val seed: Int,
    val dark: Boolean,
    val pureBlack: Boolean,
)

internal data class ColorPreviewFrame(
    val request: ColorPreviewRequest,
    val colors: ColorScheme,
)

internal typealias ColorPreviewRenderer = suspend (ColorPreviewRequest) -> ColorScheme

/**
 * One calculation at a time, with only the newest pending request retained.
 * Unlike collectLatest, input cannot keep cancelling all preview work.
 * Invalid HEX keeps the last frame; its request cannot enable Apply.
 */
internal fun Flow<ColorPreviewRequest?>.colorPreviewFrames(render: ColorPreviewRenderer): Flow<ColorPreviewFrame> =
    distinctUntilChanged().conflate().mapNotNull { request ->
        request?.let { ColorPreviewFrame(it, render(it)) }
    }
