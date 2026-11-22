package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Native window keeps keyboard, secure policy, outside dismissal and Back handling. */
@Composable
fun ModalWindow(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(decorFitsSystemWindows = false),
    strength: ModalScrimStrength = ModalScrimStrength.STANDARD,
    content: @Composable () -> Unit,
) {
    ManagedModalWindow(onDismissRequest, properties, ModalLayerKind.WINDOW, strength, false, content)
}

/** Full-screen media/composer owns an opaque/animated background, not an extra page scrim. */
@Composable
fun FullScreenModalWindow(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(decorFitsSystemWindows = false),
    replacesPageScrim: Boolean = false,
    dimUnderlying: Boolean = true,
    content: @Composable () -> Unit,
) {
    ManagedModalWindow(
        onDismissRequest,
        properties,
        ModalLayerKind.FULL_SCREEN,
        if (dimUnderlying) ModalScrimStrength.STANDARD else ModalScrimStrength.NONE,
        replacesPageScrim,
        content,
    )
}

@Composable
private fun ManagedModalWindow(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    kind: ModalLayerKind,
    strength: ModalScrimStrength,
    replacesPageScrim: Boolean,
    content: @Composable () -> Unit,
) {
    val inherited = LocalModalLayers.current
    val state = inherited ?: remember { ModalLayerState(hosted = false) }
    val token = rememberModalEntry(state, kind, strength, replacesPageScrim)
    CompositionLocalProvider(
        LocalModalLayers provides state,
        LocalModalToken provides token,
        LocalBottomOverlayInset provides 0.dp,
    ) {
        Dialog(onDismissRequest, properties = properties) {
            DisablePlatformDim(restoreDim = kind != ModalLayerKind.FULL_SCREEN || strength != ModalScrimStrength.NONE)
            val fallback =
                !state.hosted && state.first(token) &&
                    kind == ModalLayerKind.WINDOW && !state.pageScrimReplaced
            val backdrop = if (fallback) Modifier.background(Color.Black.copy(alpha = strength.alpha())) else Modifier
            val plane = if (kind == ModalLayerKind.FULL_SCREEN) Modifier.modalLayerSurface() else Modifier
            Box(Modifier.fillMaxSize().then(backdrop).then(plane)) { AppNoticeHost(content) }
        }
    }
}
