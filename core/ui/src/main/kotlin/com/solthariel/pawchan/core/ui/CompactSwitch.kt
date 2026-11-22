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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** The owning row remains the single accessible toggle target; the thumb also accepts dragging. */
@Composable
fun CompactSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val position by animateFloatAsState(if (checked) 1f else 0f, label = "switch-position")
    val radius by animateFloatAsState(
        if (checked) CHECKED_THUMB_RADIUS else UNCHECKED_THUMB_RADIUS,
        label = "switch-thumb-size",
    )
    val drag = rememberSwitchDrag(checked, onCheckedChange)
    val colors = MaterialTheme.colorScheme
    val track = if (checked) colors.primary else colors.outlineVariant
    val thumb = if (checked) colors.onPrimary else colors.onSurfaceVariant
    Box(modifier.size(48.dp, 32.dp).then(drag.modifier), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp, 24.dp)) {
            drawRoundRect(track, cornerRadius = CornerRadius(size.height / 2))
            val inset = size.height / 2
            val logicalPosition = drag.position ?: position
            val visualPosition = if (layoutDirection == LayoutDirection.Rtl) 1f - logicalPosition else logicalPosition
            drawCircle(thumb, radius.dp.toPx(), Offset(inset + (size.width - inset * 2) * visualPosition, inset))
        }
    }
}

private const val CHECKED_THUMB_RADIUS = 8f
private const val UNCHECKED_THUMB_RADIUS = 6f
