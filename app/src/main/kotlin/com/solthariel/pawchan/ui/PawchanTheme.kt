package com.solthariel.pawchan.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import com.solthariel.pawchan.core.ui.LocalTextLeadingMultiplier
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import com.solthariel.pawchan.media.LocalMediaBarState
import com.solthariel.pawchan.media.MediaBarState

@Composable
internal fun PawchanTheme(
    preference: ThemePreference,
    colors: ThemeColors = ThemeColors(),
    typography: TypographyPreferences = TypographyPreferences(),
    content: @Composable () -> Unit,
) {
    val colorScheme = pawchanColorScheme(preference, colors)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(typography),
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
            CompositionLocalProvider(
                LocalMediaBarState provides mediaBars,
                LocalTextLeadingMultiplier provides typography.leading.percent / PERCENT_DIVISOR,
            ) { content() }
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
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightNavigationBars = useDarkIcons
            isAppearanceLightStatusBars = useDarkIcons
        }
        window.isNavigationBarContrastEnforced = false
        // Фон окна — фон темы: кадр, где Compose ещё ничего не нарисовал, не мелькает чужим цветом.
        window.setBackgroundDrawable(windowColor.toDrawable())
    }
}

private const val LIGHT_ICON_THRESHOLD = 0.5f

private const val PERCENT_DIVISOR = 100f
