package com.solthariel.pawchan.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialogStyle

@Composable
internal fun PreviewWindowFrame(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val layer = LocalPreviewLayer.current
    val active = layer == null || layer.depth == 0
    val motion = layer?.motion ?: remember { PreviewSwipeState() }
    BackHandler(enabled = active, onBack = onClose)
    val dismiss =
        if (active) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
        } else {
            Modifier.clearAndSetSemantics {}
        }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .then(if (layer == null) Modifier.background(Color.Black.copy(alpha = PREVIEW_DIM)) else Modifier)
            .then(dismiss)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        PreviewCardSurface(layer, motion, maxHeight, onClose, content)
    }
}

@Composable
private fun PreviewCardSurface(
    layer: PreviewLayer?,
    motion: PreviewSwipeState,
    maxHeight: Dp,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val active = layer == null || layer.depth == 0
    val swipe = if (active) Modifier.previewSwipeModifier(motion, onClose, layer?.clear ?: onClose) else Modifier
    val viewportHeight = with(LocalDensity.current) { maxHeight.toPx() }
    var measuredHeight by remember { mutableIntStateOf(1) }
    SideEffect {
        if (active) {
            motion.height = measuredHeight.toFloat()
            motion.viewportHeight = viewportHeight
        }
    }
    Surface(
        Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight * PREVIEW_HEIGHT_FRACTION)
            .graphicsLayer {
                translationY = if (active) motion.offset else 0f
            }.then(swipe)
            .onSizeChanged { measuredHeight = it.height }
            .testTag(if (active) "preview-active" else "preview-behind")
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        shape = AppDialogStyle.shape,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
    ) {
        Box(
            Modifier.drawWithContent {
                drawContent()
                if (!active) drawRect(Color.Black.copy(alpha = PREVIEW_DIM))
            },
        ) { content() }
    }
}

internal const val PREVIEW_DIM = 0.45f
private const val PREVIEW_HEIGHT_FRACTION = 0.85f
