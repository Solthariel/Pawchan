package com.solthariel.pawchan.domain.settings

/** SYSTEM uses wallpaper colors when supported; otherwise the Lavender palette. */
enum class ThemePalette { SYSTEM, LAVENDER, BLUE, OLIVE, PEACH, ROSE, BURGUNDY, AMBER, GREEN, TEAL, GRAY, CUSTOM }

data class ThemeColors(
    val palette: ThemePalette = ThemePalette.SYSTEM,
    val pureBlack: Boolean = false,
    val customSeed: Int = DEFAULT_THEME_SEED,
) {
    init {
        require(customSeed in 0..MAX_THEME_SEED) { "Theme seed must be an opaque RGB value" }
    }
}

const val DEFAULT_THEME_SEED: Int = 0x6750A4
const val MAX_THEME_SEED: Int = 0xFFFFFF

/** Existing black overrides become the explicit, permanently dark AMOLED mode. */
fun ThemePreference.selectionMode(colors: ThemeColors): ThemePreference =
    if (this == ThemePreference.AMOLED || (colors.pureBlack && displayMode() != ThemePreference.LIGHT)) {
        ThemePreference.AMOLED
    } else {
        displayMode()
    }

/** Old names remain readable in installed preferences and portable backups only. */
fun ThemePreference.displayMode(): ThemePreference =
    when (this) {
        ThemePreference.SYSTEM -> {
            ThemePreference.SYSTEM
        }

        ThemePreference.LIGHT, ThemePreference.NORMIE, ThemePreference.BURICHAN, ThemePreference.PHOTON -> {
            ThemePreference.LIGHT
        }

        else -> {
            ThemePreference.DARK
        }
    }

fun ThemePreference.legacyColors(): ThemeColors =
    ThemeColors(
        palette =
            when (this) {
                ThemePreference.TOMORROW, ThemePreference.NORMIE, ThemePreference.NEUTRON -> ThemePalette.BLUE
                ThemePreference.HARDON, ThemePreference.PHOTON -> ThemePalette.PEACH
                else -> ThemePalette.LAVENDER
            },
        pureBlack = this == ThemePreference.AMOLED,
    )

/** Retired persisted keys remain valid in old preferences and backups. */
fun ThemePalette.currentPalette(): ThemePalette =
    when (this) {
        ThemePalette.OLIVE -> ThemePalette.AMBER
        ThemePalette.ROSE -> ThemePalette.BURGUNDY
        else -> this
    }
