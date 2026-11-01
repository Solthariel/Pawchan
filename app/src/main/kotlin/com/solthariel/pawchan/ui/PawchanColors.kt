package com.solthariel.pawchan.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.currentPalette
import com.solthariel.pawchan.domain.settings.displayMode

@Composable
internal fun pawchanColorScheme(
    preference: ThemePreference,
    colors: ThemeColors,
): ColorScheme {
    val dark =
        when (preference.displayMode()) {
            ThemePreference.SYSTEM -> isSystemInDarkTheme()
            ThemePreference.LIGHT -> false
            else -> true
        }
    val context = LocalContext.current
    // Configuration changes must refresh wallpaper-derived resources, even in a fixed light/dark mode.
    val configuration = LocalConfiguration.current
    return remember(preference, colors, dark, context, configuration) {
        val scheme =
            if (colors.palette == ThemePalette.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (colors.palette == ThemePalette.CUSTOM) {
                    generatedColorScheme(colors.customSeed, dark)
                } else {
                    presetColorScheme(colors.palette, dark)
                }
            }
        if (dark && (colors.pureBlack || preference == ThemePreference.AMOLED)) {
            scheme.withPureBlackSurfaces()
        } else {
            scheme
        }
    }
}

internal fun presetColorScheme(
    palette: ThemePalette,
    dark: Boolean,
): ColorScheme =
    when (palette.currentPalette()) {
        ThemePalette.SYSTEM, ThemePalette.LAVENDER -> if (dark) lavenderDarkColors() else lavenderLightColors()
        ThemePalette.PEACH -> if (dark) peachDarkColors() else peachLightColors()
        ThemePalette.CUSTOM -> generatedColorScheme(palette.seedColor(), dark)
        else -> softPresetColorScheme(palette.currentPalette(), dark)
    }

internal fun ColorScheme.withPureBlackSurfaces(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceDim = Color.Black,
    )
