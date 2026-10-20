package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Shared geometry, independent of font glyphs. Callers place disclosure before the label. */
@Composable
fun UiDisclosureIndicator(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier
            .size(NAVIGATION_INDICATOR_SIZE)
            .rotate(if (expanded) DISCLOSURE_ROTATION else 0f)
            .testTag("disclosure-indicator")
            .semantics { stateDescription = if (expanded) "Развёрнуто" else "Свёрнуто" },
    ) {
        val triangle =
            Path().apply {
                moveTo(size.width * INDICATOR_START, size.height * INDICATOR_TOP)
                lineTo(size.width * INDICATOR_END, size.height * INDICATOR_CENTER)
                lineTo(size.width * INDICATOR_START, size.height * INDICATOR_BOTTOM)
                close()
            }
        drawPath(triangle, color)
    }
}

/** Screen/dialog navigation: always trailing, with the same 20 dp canvas as disclosure. */
@Composable
fun UiForwardIndicator(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.size(NAVIGATION_INDICATOR_SIZE).testTag("forward-indicator")) {
        val center = Offset(size.width * INDICATOR_END, size.height * INDICATOR_CENTER)
        val stroke = INDICATOR_STROKE.dp.toPx()
        drawLine(
            color,
            Offset(size.width * INDICATOR_START, size.height * INDICATOR_TOP),
            center,
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            center,
            Offset(size.width * INDICATOR_START, size.height * INDICATOR_BOTTOM),
            stroke,
            StrokeCap.Round,
        )
    }
}

/** Return from a nested menu uses the same stroke and canvas, pointing back. */
@Composable
fun UiBackwardIndicator(modifier: Modifier = Modifier) {
    UiForwardIndicator(modifier.rotate(BACKWARD_ROTATION))
}

private val NAVIGATION_INDICATOR_SIZE = 20.dp
private const val DISCLOSURE_ROTATION = 90f
private const val INDICATOR_START = 0.32f
private const val INDICATOR_END = 0.68f
private const val INDICATOR_TOP = 0.25f
private const val INDICATOR_CENTER = 0.5f
private const val INDICATOR_BOTTOM = 0.75f
private const val INDICATOR_STROKE = 1.8f

private const val BACKWARD_ROTATION = 180f
