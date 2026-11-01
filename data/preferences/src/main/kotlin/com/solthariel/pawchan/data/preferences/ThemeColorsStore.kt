package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import com.solthariel.pawchan.domain.settings.MAX_THEME_SEED
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette
import com.solthariel.pawchan.domain.settings.legacyColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal class ThemeColorsStore(
    private val dataStore: DataStore<Preferences>,
) {
    val colors: Flow<ThemeColors> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map(Preferences::readThemeColors)

    suspend fun save(colors: ThemeColors): DomainOutcome<Unit> =
        try {
            dataStore.edit { it.writeThemeColors(colors) }
            DomainOutcome.Success(Unit)
        } catch (_: IOException) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }
}

internal fun Preferences.readThemeColors(): ThemeColors {
    val legacy = parseTheme(this[stringPreferencesKey("theme")]).legacyColors()
    val storedPalette = ThemePalette.entries.firstOrNull { it.name == this[THEME_PALETTE] } ?: legacy.palette
    return ThemeColors(
        palette = storedPalette.currentPalette(),
        pureBlack = this[THEME_PURE_BLACK] ?: legacy.pureBlack,
        customSeed = this[THEME_CUSTOM_SEED]?.takeIf { it in 0..MAX_THEME_SEED } ?: DEFAULT_THEME_SEED,
    )
}

internal fun MutablePreferences.writeThemeColors(colors: ThemeColors) {
    this[THEME_PALETTE] = colors.palette.currentPalette().name
    this[THEME_PURE_BLACK] = colors.pureBlack
    this[THEME_CUSTOM_SEED] = colors.customSeed
}

private val THEME_PALETTE = stringPreferencesKey("theme_palette")
private val THEME_PURE_BLACK = booleanPreferencesKey("theme_pure_black")

private val THEME_CUSTOM_SEED = intPreferencesKey("theme_custom_seed")
