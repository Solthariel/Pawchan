package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import com.solthariel.pawchan.domain.settings.MAX_THEME_SEED
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette
import com.solthariel.pawchan.ui.seedColor

/** Wallpaper palettes expose role colours, not their original seed. Use the current accent as an approximation. */
internal fun initialCustomSeed(
    colors: ThemeColors,
    systemAccent: Int?,
): Int =
    when (colors.palette) {
        ThemePalette.CUSTOM -> colors.customSeed
        ThemePalette.SYSTEM -> systemAccent?.and(MAX_THEME_SEED) ?: DEFAULT_THEME_SEED
        else -> colors.palette.currentPalette().seedColor()
    }
