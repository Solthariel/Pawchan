package com.solthariel.pawchan.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.legacyColors
import com.solthariel.pawchan.media.LocalMediaBarState
import com.solthariel.pawchan.media.MediaBarState

@Composable
internal fun PawchanTheme(
    preference: ThemePreference,
    colors: ThemeColors = preference.legacyColors(),
    content: @Composable () -> Unit,
) {
    val colorScheme = pawchanColorScheme(preference, colors)
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        val mediaBars = remember { MediaBarState() }
        val navigationBarColor = MaterialTheme.colorScheme.background
        SystemNavigationBar(
            background =
                com.solthariel.pawchan.core.ui
                    .modalSystemBarBackground(navigationBarColor),
            media = mediaBars.active,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalMediaBarState provides mediaBars) { content() }
            if (!mediaBars.active) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(navigationBarColor),
                )
            }
        }
    }
}

@Composable
private fun SystemNavigationBar(
    background: Color,
    media: Boolean,
) {
    val view = LocalView.current
    val useDarkIcons = !media && background.luminance() > LIGHT_ICON_THRESHOLD
    val windowColor = background.toArgb()
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkIcons
        window.isNavigationBarContrastEnforced = false
        // Фон окна — фон темы: кадр, где Compose ещё ничего не нарисовал, не мелькает чужим цветом.
        window.setBackgroundDrawable(windowColor.toDrawable())
    }
}

private const val LIGHT_ICON_THRESHOLD = 0.5f
