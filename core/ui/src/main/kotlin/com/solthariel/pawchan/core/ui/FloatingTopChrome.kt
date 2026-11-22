package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Supplied only by app screens whose content is laid out edge-to-edge under the top bar. */
val LocalFloatingTopStatusInset = staticCompositionLocalOf { 0.dp }
val LocalFloatingTopEnabled = staticCompositionLocalOf { false }
val LocalTopOverlayInset = staticCompositionLocalOf { 0.dp }
val LocalTopBarTransparency = staticCompositionLocalOf { 0 }

/** Measured toolbar size can be retained while a screen switches its content. */
@Stable
class FloatingTopChromeState internal constructor(
    initialToolbarHeight: Dp,
) {
    internal var toolbarHeight by mutableStateOf(initialToolbarHeight)
}

@Composable
fun rememberFloatingTopChromeState(initialToolbarHeight: Dp = APP_TOP_BAR_HEIGHT): FloatingTopChromeState =
    remember(initialToolbarHeight) { FloatingTopChromeState(initialToolbarHeight) }

/** Only the background is translucent; actions and labels keep their full opacity. */
@Composable
fun FloatingTopChrome(
    toolbar: @Composable () -> Unit,
    state: FloatingTopChromeState = rememberFloatingTopChromeState(),
    opaque: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!LocalFloatingTopEnabled.current) {
        Column(Modifier.fillMaxSize()) {
            toolbar()
            Box(Modifier.weight(1f)) { content() }
        }
        return
    }
    val density = LocalDensity.current
    val statusInset = LocalFloatingTopStatusInset.current
    val topInset = statusInset + state.toolbarHeight
    val transparency = if (opaque) 0 else LocalTopBarTransparency.current
    val alpha = 1f - transparency.coerceIn(0, MAX_TRANSPARENCY) / MAX_TRANSPARENCY.toFloat()
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTopOverlayInset provides topInset) { content() }
        Column(
            Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    state.toolbarHeight = (with(density) { size.height.toDp() } - statusInset).coerceAtLeast(0.dp)
                }.background(MaterialTheme.colorScheme.background.copy(alpha = alpha)),
        ) {
            Spacer(Modifier.height(statusInset))
            toolbar()
        }
    }
}

private const val MAX_TRANSPARENCY = 100
