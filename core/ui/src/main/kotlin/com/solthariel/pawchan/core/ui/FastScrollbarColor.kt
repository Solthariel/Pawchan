package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Shared idle thumb color, including opacity, for the scrollbar and post markers. */
@Composable
fun fastScrollbarThumbColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = NORMAL_THUMB_ALPHA)

private const val NORMAL_THUMB_ALPHA = 0.35f
