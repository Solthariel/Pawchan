package com.solthariel.pawchan.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/** Inline anchored panel keeps its existing keyboard/gesture owner; only its backdrop changes. */
@Composable
fun ModalInlinePanel(content: @Composable () -> Unit) {
    val state = LocalModalLayers.current
    val token = state?.let { rememberModalEntry(it, ModalLayerKind.INLINE_PANEL) }
    CompositionLocalProvider(LocalModalToken provides token, content = content)
}

/** Excludes the visible panel surface from the root scrim, without moving it to another window. */
@Composable
fun Modifier.modalPanelBounds(cornerRadius: Dp): Modifier {
    val state = LocalModalLayers.current
    val token = LocalModalToken.current
    val radius = with(LocalDensity.current) { cornerRadius.toPx() }
    return onGloballyPositioned {
        if (state != null && token != null) state.updatePanel(token, ModalPanelGeometry(it.boundsInRoot(), radius))
    }
}
