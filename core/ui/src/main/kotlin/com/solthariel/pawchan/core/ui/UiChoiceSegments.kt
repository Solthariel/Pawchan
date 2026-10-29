package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material single selection with equal cells; the last cell can open numeric input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiChoiceSegments(
    options: List<String>,
    selected: Int,
    enabled: Boolean,
    lastOptionDescription: String? = null,
    wrapLabels: Boolean = false,
    onSelect: (Int) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labelHeight = segmentLabelHeight(options, wrapLabels, maxWidth / options.size.coerceAtLeast(1))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val editable = index == options.lastIndex && lastOptionDescription != null
                SegmentedButton(
                    selected = index == selected,
                    onClick = { onSelect(index) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = (labelHeight + 16.dp).coerceAtLeast(48.dp))
                            .semantics { if (editable) contentDescription = requireNotNull(lastOptionDescription) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    icon = {},
                ) {
                    Box(Modifier.heightIn(min = labelHeight), contentAlignment = Alignment.Center) {
                        // Compose 1.12.1 String semantics can rebuild at parent width, not drawn width.
                        // AnnotatedString uses the rendered MultiParagraph for semantics as well.
                        Text(
                            remember(label) { AnnotatedString(label) },
                            textAlign = TextAlign.Center,
                            textDecoration = if (editable) TextDecoration.Underline else TextDecoration.None,
                            maxLines = if (wrapLabels) Int.MAX_VALUE else 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun segmentLabelHeight(
    options: List<String>,
    wrapLabels: Boolean,
    cellWidth: Dp,
): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    // Material 3 1.4.0 reserves an icon slot and 8 dp spacing even for an empty icon.
    val textWidth = cellWidth - SegmentedButtonDefaults.IconSize - 8.dp - 8.dp
    val constraints = Constraints(maxWidth = with(density) { textWidth.roundToPx().coerceAtLeast(1) })
    val style = MaterialTheme.typography.labelLarge
    val textHeight =
        options.maxOfOrNull { label ->
            measurer
                .measure(
                    label,
                    style,
                    maxLines = if (wrapLabels) Int.MAX_VALUE else 1,
                    constraints = constraints,
                ).size.height
        } ?: 0
    // The label must expose its measured height to Material's internal intrinsic Layout.
    // A taller outer Surface alone does not increase the label's maxHeight there.
    return with(density) { textHeight.toDp() }
}
