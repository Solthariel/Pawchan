package com.solthariel.pawchan.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.LocalMediaBarState
import com.solthariel.pawchan.media.MediaBarState

@Composable
internal fun PawchanTheme(
    preference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when (preference) {
            ThemePreference.SYSTEM -> {
                if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            }

            ThemePreference.LIGHT -> {
                lightColorScheme()
            }

            ThemePreference.DARK -> {
                darkColorScheme()
            }

            ThemePreference.AMOLED -> {
                amoledColorScheme()
            }

            ThemePreference.TOMORROW -> {
                tomorrowColorScheme()
            }

            ThemePreference.NORMIE -> {
                normieColorScheme()
            }

            ThemePreference.BURICHAN -> {
                burichanColorScheme()
            }

            ThemePreference.HARDON -> {
                hardonColorScheme()
            }

            ThemePreference.NEUTRON -> {
                neutronColorScheme()
            }

            ThemePreference.PHOTON -> {
                photonColorScheme()
            }
        }
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        val mediaBars = remember { MediaBarState() }
        val navigationBarColor = MaterialTheme.colorScheme.background
        SystemNavigationBar(background = navigationBarColor, media = mediaBars.active)
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

private fun amoledColorScheme() =
    darkColorScheme(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = AMOLED_SURFACE,
        surfaceContainer = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerHigh = AMOLED_SURFACE,
        surfaceContainerHighest = AMOLED_SURFACE,
    )

private fun tomorrowColorScheme() =
    darkColorScheme(
        primary = themeColor("#81A2BE"),
        onPrimary = themeColor("#17232D"),
        secondary = themeColor("#B5BD68"),
        tertiary = themeColor("#33CC88"),
        background = themeColor("#282A2E"),
        onBackground = themeColor("#D7D9DB"),
        surface = themeColor("#32343A"),
        onSurface = themeColor("#D7D9DB"),
        surfaceVariant = themeColor("#3A3C42"),
        onSurfaceVariant = themeColor("#B5B8BD"),
        outline = themeColor("#73767E"),
        outlineVariant = themeColor("#4A4D54"),
    )

private fun normieColorScheme() =
    lightColorScheme(
        primary = themeColor("#5181B8"),
        onPrimary = Color.White,
        secondary = themeColor("#7DA1CA"),
        tertiary = themeColor("#3E719F"),
        background = themeColor("#EEEEEE"),
        onBackground = themeColor("#1B1B1F"),
        surface = themeColor("#FAFAFA"),
        onSurface = themeColor("#1B1B1F"),
        surfaceVariant = themeColor("#E2E7ED"),
        onSurfaceVariant = themeColor("#46505B"),
        outline = themeColor("#747D87"),
        outlineVariant = themeColor("#C5CBD2"),
    )

private fun burichanColorScheme() =
    lightColorScheme(
        primary = themeColor("#7777BB"),
        onPrimary = Color.White,
        secondary = themeColor("#AF0A0F"),
        tertiary = themeColor("#779922"),
        background = themeColor("#EEF2FF"),
        onBackground = themeColor("#222236"),
        surface = themeColor("#F7F9FF"),
        onSurface = themeColor("#222236"),
        surfaceVariant = themeColor("#E2E6F5"),
        onSurfaceVariant = themeColor("#4D5064"),
        outline = themeColor("#76798D"),
        outlineVariant = themeColor("#C7CADB"),
    )

private fun hardonColorScheme() =
    darkColorScheme(
        primary = themeColor("#FF6600"),
        onPrimary = Color.Black,
        secondary = themeColor("#77DD00"),
        tertiary = themeColor("#33CC88"),
        background = themeColor("#181818"),
        onBackground = themeColor("#E6E1E5"),
        surface = themeColor("#303030"),
        onSurface = themeColor("#E6E1E5"),
        surfaceVariant = themeColor("#383838"),
        onSurfaceVariant = themeColor("#C8C3C7"),
        outline = themeColor("#938F93"),
        outlineVariant = themeColor("#494649"),
    )

private fun neutronColorScheme() =
    darkColorScheme(
        primary = themeColor("#6688CC"),
        onPrimary = themeColor("#10203F"),
        secondary = themeColor("#CCBB88"),
        tertiary = themeColor("#779922"),
        background = themeColor("#2C2C2C"),
        onBackground = themeColor("#6688CC"),
        surface = themeColor("#202020"),
        onSurface = themeColor("#6688CC"),
        surfaceVariant = themeColor("#30343A"),
        onSurfaceVariant = themeColor("#CCBB88"),
        outline = themeColor("#687080"),
        outlineVariant = themeColor("#3E4654"),
    )

private fun photonColorScheme() =
    lightColorScheme(
        primary = themeColor("#FF6600"),
        onPrimary = Color.Black,
        secondary = themeColor("#779922"),
        tertiary = themeColor("#228855"),
        background = themeColor("#EEEEEE"),
        onBackground = themeColor("#1B1B1F"),
        surface = themeColor("#FAFAFA"),
        onSurface = themeColor("#1B1B1F"),
        surfaceVariant = themeColor("#E8E2DD"),
        onSurfaceVariant = themeColor("#554D47"),
        outline = themeColor("#83746A"),
        outlineVariant = themeColor("#D3C5BB"),
    )

private fun themeColor(value: String): Color = Color(value.toColorInt())

private val AMOLED_SURFACE = Color(AMOLED_SURFACE_ARGB)
private const val LIGHT_ICON_THRESHOLD = 0.5f
private const val AMOLED_SURFACE_ARGB = 0xFF101010
