package com.solthariel.pawchan.media

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Owned by the theme, supports an activity viewer and a dialog viewer without global state. */
class MediaBarState {
    private var owners by mutableStateOf(emptySet<Any>())
    val active: Boolean get() = owners.isNotEmpty()

    fun acquire(owner: Any) {
        owners = owners + owner
    }

    fun release(owner: Any) {
        owners = owners - owner
    }
}

val LocalMediaBarState = staticCompositionLocalOf<MediaBarState?> { null }

@Composable
internal fun MediaSystemBars(visible: Boolean) {
    val view = LocalView.current
    val bars = LocalMediaBarState.current
    val token = remember { Any() }
    val window = (view.parent as? DialogWindowProvider)?.window ?: (view.context as? Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    SideEffect {
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(view, bars) {
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNavigation = controller?.isAppearanceLightNavigationBars
        val behavior = controller?.systemBarsBehavior
        val statusVisible =
            view.rootWindowInsets?.isVisible(
                android.view.WindowInsets.Type
                    .statusBars(),
            ) != false
        val navigationVisible =
            view.rootWindowInsets?.isVisible(
                android.view.WindowInsets.Type
                    .navigationBars(),
            ) != false
        val navigationContrast = window?.isNavigationBarContrastEnforced
        bars?.acquire(token)
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        window?.isNavigationBarContrastEnforced = false
        onDispose {
            bars?.release(token)
            behavior?.let { controller?.systemBarsBehavior = it }
            if (statusVisible) {
                controller?.show(
                    WindowInsetsCompat.Type.statusBars(),
                )
            } else {
                controller?.hide(WindowInsetsCompat.Type.statusBars())
            }
            if (navigationVisible) {
                controller?.show(
                    WindowInsetsCompat.Type.navigationBars(),
                )
            } else {
                controller?.hide(WindowInsetsCompat.Type.navigationBars())
            }
            lightStatus?.let { controller?.isAppearanceLightStatusBars = it }
            lightNavigation?.let { controller?.isAppearanceLightNavigationBars = it }
            navigationContrast?.let { window?.isNavigationBarContrastEnforced = it }
        }
    }
}
