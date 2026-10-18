package com.solthariel.pawchan.media

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** Media overlay color; independent of the app theme. */
internal val MEDIA_CHROME_BACKGROUND = Color(MEDIA_CHROME_ARGB)
internal val MEDIA_CHROME_COLORS =
    darkColorScheme(
        background = Color(MEDIA_CHROME_BASE_ARGB),
        onBackground = Color.White,
        onSurfaceVariant = Color(MEDIA_CHROME_SECONDARY_ARGB),
    )

internal const val MEDIA_CHROME_ARGB = 0xAA202020
private const val MEDIA_CHROME_BASE_ARGB = 0xFF202020
private const val MEDIA_CHROME_SECONDARY_ARGB = 0xFFAAAAAA
