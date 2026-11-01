package com.solthariel.pawchan.ui

import androidx.compose.material3.ColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.Variant
import com.solthariel.pawchan.domain.settings.ThemePalette

/** Muted preset chroma is deliberate; Tonal Spot would re-saturate a pale source color. */
internal fun softPresetColorScheme(
    palette: ThemePalette,
    dark: Boolean,
): ColorScheme {
    val source = Hct.fromInt(palette.seedColor() or OPAQUE_PRESET_ALPHA)
    val hue = source.hue
    val chroma = palette.presetChroma()
    return DynamicScheme(
        sourceColorHct = source,
        variant = Variant.TONAL_SPOT,
        isDark = dark,
        contrastLevel = 0.0,
        primaryPalette = TonalPalette.fromHueAndChroma(hue, chroma),
        secondaryPalette = TonalPalette.fromHueAndChroma(hue, minOf(chroma, SECONDARY_CHROMA)),
        tertiaryPalette = TonalPalette.fromHueAndChroma(hue, minOf(chroma, TERTIARY_CHROMA)),
        neutralPalette = TonalPalette.fromHueAndChroma(hue, minOf(chroma, NEUTRAL_CHROMA)),
        neutralVariantPalette = TonalPalette.fromHueAndChroma(hue, minOf(chroma, VARIANT_CHROMA)),
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
    ).toComposeColors()
}

private fun ThemePalette.presetChroma(): Double =
    when (this) {
        ThemePalette.BLUE -> CORNFLOWER_CHROMA
        ThemePalette.BURGUNDY -> CORAL_CHROMA
        ThemePalette.GRAY -> PEBBLE_CHROMA
        else -> SOFT_CHROMA
    }

private const val OPAQUE_PRESET_ALPHA = -0x1000000
private const val CORNFLOWER_CHROMA = 32.0
private const val CORAL_CHROMA = 40.0
private const val SOFT_CHROMA = 24.0
private const val PEBBLE_CHROMA = 2.0
private const val SECONDARY_CHROMA = 12.0
private const val TERTIARY_CHROMA = 16.0
private const val NEUTRAL_CHROMA = 4.0
private const val VARIANT_CHROMA = 6.0
