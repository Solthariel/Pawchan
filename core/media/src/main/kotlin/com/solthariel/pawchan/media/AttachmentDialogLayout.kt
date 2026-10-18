package com.solthariel.pawchan.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/** Only used in the attachment editor, never in catalog/post previews. */
@Composable
fun AttachmentDialogLayout(
    content: AttachmentDialogContent,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val gap = 8.dp.roundToPx()
        val natural = Constraints(minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth)
        val header = subcompose("header", content.header).single().measure(natural)
        val budget = (constraints.maxHeight - header.height - gap * 2).coerceAtLeast(0)
        val footer = subcompose("footer", content.footer).single().measure(natural.copy(maxHeight = budget))
        val metrics =
            attachmentDialogMetrics(
                constraints.maxHeight,
                header.height,
                footer.height,
                gap,
                constraints.maxWidth,
                content.aspectRatio,
            )
        val mediaHeight =
            if (content.previewHeightLimit.isSpecified) {
                minOf(
                    metrics.mediaHeight,
                    content.previewHeightLimit.roundToPx(),
                    constraints.maxHeight / PREVIEW_MAX_HEIGHT_DIVISOR,
                )
            } else {
                metrics.mediaHeight
            }
        val media =
            subcompose("media", content.media).single().measure(
                Constraints.fixed(constraints.maxWidth, mediaHeight),
            )
        layout(constraints.maxWidth, metrics.totalHeight - metrics.mediaHeight + mediaHeight) {
            header.placeRelative(0, 0)
            media.placeRelative(0, header.height + gap)
            footer.placeRelative(0, header.height + gap * 2 + mediaHeight)
        }
    }
}

private const val PREVIEW_MAX_HEIGHT_DIVISOR = 3
