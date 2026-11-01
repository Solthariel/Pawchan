package com.solthariel.pawchan.core.ui

import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/** No platform scrim may multiply the host's own scrim. Restore only the attributes owned here. */
@Composable
internal fun DisablePlatformDim() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    val background = modalSystemBarBackground(MaterialTheme.colorScheme.background)
    DisposableEffect(view) {
        val previous = window?.attributes?.dimAmount
        val contrast = window?.isNavigationBarContrastEnforced
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightNavigation = controller?.isAppearanceLightNavigationBars
        window?.let { WindowCompat.enableEdgeToEdge(it) }
        window?.isNavigationBarContrastEnforced = false
        val hadDim = window != null && window.attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
        window?.setDimAmount(0f)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        onDispose {
            previous?.let { window.setDimAmount(it) }
            contrast?.let { window.isNavigationBarContrastEnforced = it }
            lightNavigation?.let { controller.isAppearanceLightNavigationBars = it }
            if (hadDim) window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }
    SideEffect {
        window?.let {
            val darkIcons = background.luminance() > LIGHT_NAVIGATION_THRESHOLD
            WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = darkIcons
        }
    }
}

private const val LIGHT_NAVIGATION_THRESHOLD = 0.5f
