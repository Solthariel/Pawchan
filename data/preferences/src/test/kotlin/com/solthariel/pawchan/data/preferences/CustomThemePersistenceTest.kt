package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.currentPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomThemePersistenceTest {
    @Test fun customColorSurvivesModeChangesAndStoreRecreation() =
        runBlocking {
            val data = CustomThemeMemoryStore()
            val repository = DataStoreSettingsRepository(data)
            val colors = ThemeColors(ThemePalette.CUSTOM, customSeed = CUSTOM_RGB)
            repository.setThemeColors(colors)
            val modes =
                listOf(
                    ThemePreference.AMOLED,
                    ThemePreference.LIGHT,
                    ThemePreference.SYSTEM,
                    ThemePreference.DARK,
                )
            modes.forEach {
                repository.setTheme(it)
                assertEquals(colors.copy(pureBlack = it == ThemePreference.AMOLED), repository.themeColors.first())
                assertEquals(it, DataStoreSettingsRepository(data).theme.first())
            }
            assertEquals(colors, DataStoreSettingsRepository(data).themeColors.first())
        }

    @Test fun existingSystemBlackOverrideBecomesAmoledWithoutLosingPalette() =
        runBlocking {
            val data =
                CustomThemeMemoryStore(
                    mutablePreferencesOf(
                        stringPreferencesKey("theme") to "SYSTEM",
                        stringPreferencesKey("theme_palette") to "TEAL",
                        booleanPreferencesKey("theme_pure_black") to true,
                    ),
                )
            val repository = DataStoreSettingsRepository(data)
            assertEquals(ThemePreference.AMOLED, repository.theme.first())
            assertEquals(ThemePalette.TEAL, repository.themeColors.first().palette)
            repository.setTheme(ThemePreference.AMOLED)
            assertEquals(ThemePalette.TEAL, repository.themeColors.first().palette)
        }

    @Test fun allNewPalettesAndCustomSeedRoundTripThroughBackup() =
        runBlocking {
            ThemePalette.entries.forEach { palette ->
                val original = CustomThemeMemoryStore()
                val input = ThemeColors(palette, true, CUSTOM_RGB)
                val expected = input.copy(palette = palette.currentPalette())
                ThemeColorsStore(original).save(input)
                val values = DataStorePortableSettings(original) {}.export()
                val restored = CustomThemeMemoryStore()
                DataStorePortableSettings(restored) {}.replace(values, null)
                assertEquals(expected, restored.data.first().readThemeColors())
            }
        }

    @Test fun invalidBackupSeedIsRejectedBeforeMutation() =
        runBlocking {
            listOf("-1", "16777216", "not-a-color").forEach { seed ->
                val data = CustomThemeMemoryStore()
                val portable = DataStorePortableSettings(data) {}
                val result = runCatching { portable.replace(mapOf("theme_custom_seed" to seed), null) }
                assertTrue(result.exceptionOrNull() is IllegalArgumentException)
                assertEquals(emptyPreferences(), data.data.first())
            }
        }

    @Test fun corruptStoredSeedFallsBackAndMissingSeedKeepsOldBackupCompatible() =
        runBlocking {
            val stored = mutablePreferencesOf(intPreferencesKey("theme_custom_seed") to -1)
            assertEquals(DEFAULT_THEME_SEED, stored.readThemeColors().customSeed)
            val data = CustomThemeMemoryStore()
            ThemeColorsStore(data).save(ThemeColors(ThemePalette.CUSTOM, customSeed = CUSTOM_RGB))
            DataStorePortableSettings(data) {}.replace(mapOf("theme_palette" to "BLUE"), null)
            assertEquals(
                ThemePalette.BLUE,
                data.data
                    .first()
                    .readThemeColors()
                    .palette,
            )
            assertEquals(
                DEFAULT_THEME_SEED,
                data.data
                    .first()
                    .readThemeColors()
                    .customSeed,
            )
        }

    @Test fun unrelatedPartialBackupDoesNotChangeCustomColor() =
        runBlocking {
            val data = CustomThemeMemoryStore()
            val expected = ThemeColors(ThemePalette.CUSTOM, customSeed = CUSTOM_RGB)
            ThemeColorsStore(data).save(expected)
            DataStorePortableSettings(data) {}.replace(mapOf("home_board" to ""), null)
            assertEquals(expected, data.data.first().readThemeColors())
            assertNotEquals(DEFAULT_THEME_SEED, expected.customSeed)
        }
}

private class CustomThemeMemoryStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    override val data = MutableStateFlow(initial)

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}

private const val CUSTOM_RGB = 0x23A080
