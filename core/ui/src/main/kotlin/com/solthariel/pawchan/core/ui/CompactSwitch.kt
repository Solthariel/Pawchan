package com.solthariel.pawchan.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/** Visual only: the owning row supplies one full-width, accessible toggle target. */
@Composable
fun CompactSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val position by animateFloatAsState(if (checked) 1f else 0f, label = "switch-position")
    val colors = MaterialTheme.colorScheme
    val track = if (checked) colors.primary else colors.outlineVariant
    val thumb = if (checked) colors.onPrimary else colors.onSurfaceVariant
    Box(modifier.size(48.dp, 32.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp, 24.dp)) {
            drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2))
            val radius = 8.dp.toPx()
            val inset = size.height / 2
            drawCircle(thumb, radius, Offset(inset + (size.width - inset * 2) * position, inset))
        }
    }
}
