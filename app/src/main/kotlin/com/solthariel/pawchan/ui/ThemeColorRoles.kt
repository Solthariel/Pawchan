package com.solthariel.pawchan.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Complete author palettes without inheriting unrelated default purple containers. */
internal fun ColorScheme.withCustomMaterialRoles(dark: Boolean): ColorScheme {
    val primaryFill = lerp(surface, primary, if (dark) DARK_PRIMARY_FILL else LIGHT_PRIMARY_FILL)
    val secondaryFill = lerp(surface, secondary, if (dark) DARK_ACCENT_FILL else LIGHT_ACCENT_FILL)
    val tertiaryFill = lerp(surface, tertiary, if (dark) DARK_ACCENT_FILL else LIGHT_ACCENT_FILL)
    val primaryFixedFill = lerp(Color.White, primary, FIXED_FILL)
    val secondaryFixedFill = lerp(Color.White, secondary, FIXED_FILL)
    val tertiaryFixedFill = lerp(Color.White, tertiary, FIXED_FILL)
    val inverse = if (dark) Color(LIGHT_INVERSE_ARGB) else Color(DARK_INVERSE_ARGB)
    return copy(
        onPrimary = primary.readableForeground(),
        primaryContainer = primaryFill,
        onPrimaryContainer = primaryFill.readableForeground(),
        onSecondary = secondary.readableForeground(),
        secondaryContainer = secondaryFill,
        onSecondaryContainer = secondaryFill.readableForeground(),
        onTertiary = tertiary.readableForeground(),
        tertiaryContainer = tertiaryFill,
        onTertiaryContainer = tertiaryFill.readableForeground(),
        primaryFixed = primaryFixedFill,
        primaryFixedDim = lerp(Color.White, primary, FIXED_DIM_FILL),
        onPrimaryFixed = Color.Black,
        onPrimaryFixedVariant = Color.Black,
        secondaryFixed = secondaryFixedFill,
        secondaryFixedDim = lerp(Color.White, secondary, FIXED_DIM_FILL),
        onSecondaryFixed = Color.Black,
        onSecondaryFixedVariant = Color.Black,
        tertiaryFixed = tertiaryFixedFill,
        tertiaryFixedDim = lerp(Color.White, tertiary, FIXED_DIM_FILL),
        onTertiaryFixed = Color.Black,
        onTertiaryFixedVariant = Color.Black,
        surfaceTint = primary,
        surfaceDim = if (dark) background else lerp(surface, onSurface, DIM_SURFACE_FILL),
        surfaceBright = if (dark) lerp(surface, onSurface, BRIGHT_SURFACE_FILL) else surface,
        surfaceContainerLowest = background,
        surfaceContainerLow = lerp(background, surface, LOW_SURFACE_FILL),
        surfaceContainer = surface,
        surfaceContainerHigh = lerp(surface, surfaceVariant, HIGH_SURFACE_FILL),
        surfaceContainerHighest = surfaceVariant,
        inverseSurface = inverse,
        inverseOnSurface = inverse.readableForeground(),
        inversePrimary = inverse.readableForeground(),
    )
}

/** Select the higher-contrast opaque foreground, including pale secondary accents. */
private fun Color.readableForeground(): Color = if (luminance() > BLACK_WHITE_CROSSOVER) Color.Black else Color.White

private const val BLACK_WHITE_CROSSOVER = 0.17912878f

private const val DARK_PRIMARY_FILL = 0.30f
private const val LIGHT_PRIMARY_FILL = 0.16f
private const val DARK_ACCENT_FILL = 0.24f
private const val LIGHT_ACCENT_FILL = 0.14f
private const val FIXED_FILL = 0.20f
private const val FIXED_DIM_FILL = 0.35f
private const val LIGHT_INVERSE_ARGB = 0xFFE6E1E5
private const val DARK_INVERSE_ARGB = 0xFF303034
private const val DIM_SURFACE_FILL = 0.12f
private const val BRIGHT_SURFACE_FILL = 0.10f
private const val LOW_SURFACE_FILL = 0.50f
private const val HIGH_SURFACE_FILL = 0.60f
