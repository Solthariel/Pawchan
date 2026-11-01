package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.currentPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ThemeColorsStoreTest {
    @Test fun missingAndUnknownPaletteUseLegacyFallback() {
        val theme = stringPreferencesKey("theme")
        val palette = stringPreferencesKey("theme_palette")
        assertEquals(ThemeColors(), emptyPreferences().readThemeColors())
        assertEquals(ThemeColors(ThemePalette.BLUE), mutablePreferencesOf(theme to "TOMORROW").readThemeColors())
        assertEquals(ThemeColors(), mutablePreferencesOf(palette to "BROKEN").readThemeColors())
    }

    @Test fun explicitBlackFalseOverridesLegacyAmoled() {
        val prefs =
            mutablePreferencesOf(
                stringPreferencesKey("theme") to "AMOLED",
                booleanPreferencesKey("theme_pure_black") to false,
            )
        assertEquals(ThemeColors(), prefs.readThemeColors())
    }

    @Test fun savedColorsSurviveNewStoreAndKeepUnrelatedValues() =
        runBlocking {
            val theme = stringPreferencesKey("theme")
            val unrelated = stringPreferencesKey("download_tree")
            val data = ColorMemoryStore(mutablePreferencesOf(theme to "DARK", unrelated to "content://kept"))
            val expected = ThemeColors(ThemePalette.BURGUNDY, true)
            assertEquals(DomainOutcome.Success(Unit), ThemeColorsStore(data).save(expected))
            assertEquals(expected, ThemeColorsStore(data).colors.first())
            assertEquals("DARK", data.data.first()[theme])
            assertEquals("content://kept", data.data.first()[unrelated])
        }

    @Test fun everyPaletteRoundTrips() =
        runBlocking {
            val store = ThemeColorsStore(ColorMemoryStore())
            ThemePalette.entries.forEach { palette ->
                listOf(false, true).forEach { black ->
                    val expected = ThemeColors(palette.currentPalette(), black)
                    store.save(expected)
                    assertEquals(expected, store.colors.first())
                }
            }
        }

    @Test fun ioFailureIsReportedWithoutChangingSelection() =
        runBlocking {
            val data = ColorMemoryStore()
            data.writeFailure = IOException("full")
            val store = ThemeColorsStore(data)
            assertEquals(
                DomainOutcome.Failure(DomainFailure.StorageUnavailable),
                store.save(ThemeColors(ThemePalette.BLUE)),
            )
            assertEquals(ThemeColors(), store.colors.first())
        }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToStorageError() =
        runBlocking {
            val data = ColorMemoryStore()
            data.writeFailure = CancellationException("cancel")
            ThemeColorsStore(data).save(ThemeColors(ThemePalette.OLIVE))
            Unit
        }

    @Test fun readIoFailureUsesSafeDefault() =
        runBlocking {
            val data =
                object : DataStore<Preferences> {
                    override val data = kotlinx.coroutines.flow.flow<Preferences> { throw IOException("unavailable") }

                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                        throw IOException("unavailable")
                }
            assertEquals(ThemeColors(), ThemeColorsStore(data).colors.first())
        }

    @Test fun portableRoundTripKeepsColorsAndBlackOverride() =
        runBlocking {
            val original = ColorMemoryStore()
            val colors = ThemeColors(ThemePalette.PEACH, true)
            ThemeColorsStore(original).save(colors)
            val values = DataStorePortableSettings(original) {}.export()
            val restored = ColorMemoryStore()
            DataStorePortableSettings(restored) {}.replace(values, null)
            assertEquals(colors, restored.data.first().readThemeColors())
        }

    @Test fun oldBackupReplacesPreviousPaletteWithoutTouchingOtherSettings() =
        runBlocking {
            val data = ColorMemoryStore(mutablePreferencesOf(stringPreferencesKey("theme_palette") to "ROSE"))
            val portable = DataStorePortableSettings(data) {}
            portable.replace(mapOf("theme" to "AMOLED"), null)
            assertEquals(ThemeColors(ThemePalette.LAVENDER, true), data.data.first().readThemeColors())
            portable.replace(mapOf("theme" to "TOMORROW"), null)
            assertEquals(ThemeColors(ThemePalette.BLUE), data.data.first().readThemeColors())
        }

    @Test fun badBackupPaletteIsRejectedBeforeMutation() =
        runBlocking {
            val data = ColorMemoryStore()
            val portable = DataStorePortableSettings(data) {}
            var rejected = false
            try {
                portable.replace(mapOf("theme_palette" to "UNKNOWN"), null)
            } catch (_: IllegalArgumentException) {
                rejected = true
            }
            assertTrue(rejected)
            assertEquals(emptyPreferences(), data.data.first())
        }
}

private class ColorMemoryStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    override val data = MutableStateFlow(initial)
    var writeFailure: Exception? = null

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        writeFailure?.let { throw it }
        return transform(data.value).also { data.value = it }
    }
}
