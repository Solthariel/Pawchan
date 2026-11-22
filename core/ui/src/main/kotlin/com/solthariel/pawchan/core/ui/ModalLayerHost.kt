package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics

internal val LocalModalLayers = staticCompositionLocalOf<ModalLayerState?> { null }
internal val LocalModalToken = staticCompositionLocalOf<Any?> { null }

/** One owner per app UI tree. Native windows and popups inherit its composition locals. */
@Composable
fun ModalLayerHost(content: @Composable () -> Unit) {
    val state = remember { ModalLayerState(hosted = true) }
    CompositionLocalProvider(LocalModalLayers provides state) {
        var origin by remember { mutableStateOf(Offset.Zero) }
        val semantics =
            if (state.pageAlpha() > 0f && state.panel() == null) {
                Modifier.clearAndSetSemantics {}
            } else {
                Modifier
            }
        Box(
            Modifier
                .fillMaxSize()
                .testTag("modal-layer-host")
                .then(semantics)
                .onGloballyPositioned { origin = it.positionInRoot() }
                .modalHostBackdrop(state) { origin },
            content = { content() },
        )
    }
}

@Composable
internal fun rememberModalEntry(
    state: ModalLayerState,
    kind: ModalLayerKind,
    strength: ModalScrimStrength = ModalScrimStrength.STANDARD,
    replacesPageScrim: Boolean = false,
): Any {
    val token = remember(state, kind, strength, replacesPageScrim) { Any() }
    DisposableEffect(state, token) {
        state.enter(ModalLayerEntry(token, kind, strength, replacesPageScrim = replacesPageScrim))
        onDispose { state.leave(token) }
    }
    return token
}

/** Attach only to the visible surface, never the full transparent dialog viewport. */
@Composable
fun Modifier.modalLayerSurface(shape: Shape? = null): Modifier {
    val state = LocalModalLayers.current
    val token = LocalModalToken.current
    val covered = state != null && token != null && state.covered(token)
    val dim = if (state != null && token != null) state.coveredAlpha(token) else 0f
    return modalDimmedPlane(dim, covered, shape)
}

private fun Modifier.modalDimmedPlane(
    dim: Float,
    covered: Boolean,
    shape: Shape? = null,
): Modifier =
    if (dim > 0f) {
        clearAndSetSemantics {}.drawWithCache {
            val outline = shape?.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                val color = Color.Black.copy(alpha = dim)
                when {
                    outline != null -> drawOutline(outline, color)
                    else -> drawRect(color)
                }
            }
        }
    } else if (covered) {
        clearAndSetSemantics {}
    } else {
        this
    }

/** Effective background for system-button contrast; the host itself draws the actual overlay. */
@Composable
fun modalSystemBarBackground(background: Color): Color {
    val alpha = LocalModalLayers.current?.systemBarAlpha() ?: 0f
    return Color.Black.copy(alpha = alpha).compositeOver(background)
}
