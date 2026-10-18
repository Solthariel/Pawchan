package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Compact visuals retain Material's keyboard, accessibility and minimum touch target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(Modifier.size(width = 4.dp, height = 24.dp).background(colors.primary, RoundedCornerShape(2.dp)))
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val fraction =
                    ((state.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
                val start = Offset(if (rtl) size.width else 0f, center.y)
                val end = Offset(if (rtl) 0f else size.width, center.y)
                val thumb = Offset(if (rtl) size.width * (1f - fraction) else size.width * fraction, center.y)
                drawLine(colors.secondaryContainer, start, end, size.height, StrokeCap.Round)
                drawLine(colors.primary, start, thumb, size.height, StrokeCap.Round)
            }
        },
    )
}
