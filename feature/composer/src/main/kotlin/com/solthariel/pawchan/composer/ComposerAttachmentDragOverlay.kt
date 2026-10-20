package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.ImageLoader
import kotlin.math.roundToInt

@Composable
internal fun ComposerAttachmentDragOverlay(
    file: ComposerAttachment,
    loader: ImageLoader,
    grid: ComposerDropGrid,
    drag: ComposerAttachmentDragState,
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .absoluteOffset { IntOffset(drag.topLeft.x.roundToInt(), drag.topLeft.y.roundToInt()) }
            .size(with(density) { grid.cellWidth.toDp() }, with(density) { grid.cellHeight.toDp() })
            .zIndex(1f)
            .graphicsLayer {
                scaleX = DRAG_SCALE
                scaleY = DRAG_SCALE
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(8.dp)
            }.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {},
    ) {
        ComposerAttachmentPreview(file, loader, Modifier.fillMaxSize())
    }
}

private const val DRAG_SCALE = 1.04f
