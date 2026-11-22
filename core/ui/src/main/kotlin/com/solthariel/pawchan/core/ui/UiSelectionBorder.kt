package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Shared selection outline for radio choices and source tabs. */
@Composable
fun uiSelectionBorder(
    selected: Boolean,
    enabled: Boolean,
): BorderStroke {
    val colors = MaterialTheme.colorScheme
    val active = selected && enabled
    val color =
        when {
            !enabled -> colors.onSurfaceVariant.copy(alpha = DISABLED_BORDER_ALPHA)
            active -> colors.primary
            else -> colors.outline
        }
    return BorderStroke(if (active) ACTIVE_BORDER_WIDTH else DEFAULT_BORDER_WIDTH, color)
}

private val ACTIVE_BORDER_WIDTH = 2.5.dp
private val DEFAULT_BORDER_WIDTH = 1.dp
private const val DISABLED_BORDER_ALPHA = 0.55f
