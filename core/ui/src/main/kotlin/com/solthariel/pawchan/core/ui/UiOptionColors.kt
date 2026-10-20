package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same neutral surface as directory rows. Selection changes outline and icon, not fill or caption. */
@Composable
fun uiOptionColors(
    selected: Boolean,
    enabled: Boolean,
): UiOptionColors {
    val colors = MaterialTheme.colorScheme
    val ordinary = colors.onSurfaceVariant.copy(alpha = if (enabled) 1f else UNAVAILABLE_OPTION_ALPHA)
    val accent = if (selected && enabled) colors.primary else ordinary
    return UiOptionColors(
        colors.surfaceContainerLow,
        accent,
        colors.onSurface.copy(alpha = if (enabled) 1f else UNAVAILABLE_OPTION_ALPHA),
    )
}

data class UiOptionColors(
    val background: Color,
    val foreground: Color,
    val caption: Color,
)

private const val UNAVAILABLE_OPTION_ALPHA = 0.55f
