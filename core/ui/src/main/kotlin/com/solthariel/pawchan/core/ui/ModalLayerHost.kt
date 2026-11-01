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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
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
        val panel = state.panel()
        val cutout = panel?.let { RoundRect(it.bounds.translate(-origin), CornerRadius(it.radius)) }
        Box(
            Modifier
                .fillMaxSize()
                .testTag("modal-layer-host")
                .onGloballyPositioned { origin = it.positionInRoot() }
                .modalDimmedPlane(state.pageAlpha(), cutout = cutout),
            content = { content() },
        )
    }
}

@Composable
internal fun rememberModalEntry(
    state: ModalLayerState,
    kind: ModalLayerKind,
    strength: ModalScrimStrength = ModalScrimStrength.STANDARD,
): Any {
    val token = remember(state, kind, strength) { Any() }
    DisposableEffect(state, token) {
        state.enter(ModalLayerEntry(token, kind, strength))
        onDispose { state.leave(token) }
    }
    return token
}

/** Attach only to the visible surface, never the full transparent dialog viewport. */
@Composable
fun Modifier.modalLayerSurface(shape: Shape? = null): Modifier {
    val state = LocalModalLayers.current
    val token = LocalModalToken.current
    return modalDimmedPlane(if (state != null && token != null) state.coveredAlpha(token) else 0f, shape)
}

private fun Modifier.modalDimmedPlane(
    dim: Float,
    shape: Shape? = null,
    cutout: RoundRect? = null,
): Modifier =
    if (dim > 0f) {
        val semantics = if (cutout == null) clearAndSetSemantics {} else this
        semantics.drawWithCache {
            val path = cutout?.let { Path().apply { addRoundRect(it) } }
            val outline = shape?.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                val color = Color.Black.copy(alpha = dim)
                when {
                    path != null -> clipPath(path, ClipOp.Difference) { drawRect(color) }
                    outline != null -> drawOutline(outline, color)
                    else -> drawRect(color)
                }
            }
        }
    } else {
        this
    }

/** Effective background for system-button contrast; the host itself draws the actual overlay. */
@Composable
fun modalSystemBarBackground(background: Color): Color {
    val alpha = LocalModalLayers.current?.systemBarAlpha() ?: 0f
    return Color.Black.copy(alpha = alpha).compositeOver(background)
}
