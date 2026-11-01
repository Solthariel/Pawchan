package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Inline preview stack supplies its own viewport backdrop; its cards are separate dimmable planes. */
@Composable
fun ModalInlineLayer(content: @Composable () -> Unit) {
    val inherited = LocalModalLayers.current
    val state = inherited ?: remember { ModalLayerState(hosted = false) }
    val token = rememberModalEntry(state, ModalLayerKind.INLINE)
    CompositionLocalProvider(LocalModalLayers provides state, LocalModalToken provides token, content = content)
}

@Composable
fun Modifier.modalInlineBackdrop(): Modifier {
    val state = LocalModalLayers.current
    val token = LocalModalToken.current
    val root = state == null || token == null || state.first(token)
    return if (root) background(Color.Black.copy(alpha = MODAL_SCRIM_ALPHA)) else this
}
