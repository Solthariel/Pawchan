package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Reserve only the measured hint, not three empty text lines. Both search modes share the same minimum. */
@Composable
internal fun filterInputMinimumHeight(
    presentation: ContentFilterInputPresentation,
    width: Dp,
): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val hints = presentation.hintAlternatives.ifEmpty { listOf(presentation.hint) }.filter(String::isNotEmpty)
    return remember(hints, width, density, measurer, style) {
        with(density) {
            val height =
                hints.maxOfOrNull { hint ->
                    measurer
                        .measure(
                            AnnotatedString(hint),
                            style = style,
                            constraints = Constraints(maxWidth = width.roundToPx().coerceAtLeast(1)),
                            maxLines = 3,
                        ).size.height
                } ?: 0
            maxOf(36.dp, height.toDp() + 16.dp)
        }
    }
}
