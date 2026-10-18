package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortableSettingsTest {
    @Test fun defaultsAreExportedAndSecretsAndLocalPathsAreNot() =
        runBlocking {
            val store =
                memoryStore(
                    mutablePreferencesOf(
                        stringPreferencesKey("download_tree") to "content://private",
                        stringPreferencesKey("token") to "secret",
                        stringPreferencesKey("pin") to "1234",
                    ),
                )
            val values = DataStorePortableSettings(store) {}.export()
            assertTrue(values.isNotEmpty())
            assertEquals("SYSTEM", values["theme"])
            assertFalse(values.keys.any { it in setOf("download_tree", "token", "pin") })
        }

    @Test fun replacingSelectedPreferencesKeepsOtherSections() =
        runBlocking {
            val store =
                memoryStore(
                    mutablePreferencesOf(
                        stringPreferencesKey("theme") to "AMOLED",
                        stringPreferencesKey("favorite_boards_v2") to "b",
                        stringPreferencesKey("download_tree") to "content://private",
                    ),
                )
            val portable = DataStorePortableSettings(store) {}
            portable.replace(mapOf("favorite_boards_v2" to ""), null)
            val saved = store.data.first()
            assertEquals("AMOLED", saved[stringPreferencesKey("theme")])
            assertEquals("", saved[stringPreferencesKey("favorite_boards_v2")])
            assertEquals("content://private", saved[stringPreferencesKey("download_tree")])
        }

    @Test fun replacingOwnMarkersDoesNotTouchHiddenPosts() =
        runBlocking {
            val own = stringSetPreferencesKey("post:dvach:b:42:own")
            val hidden = stringSetPreferencesKey("post:dvach:b:42:hidden")
            val store = memoryStore(mutablePreferencesOf(own to setOf("43"), hidden to setOf("44")))
            DataStorePortableSettings(store) {}.replace(emptyMap(), emptyMap())
            assertFalse(store.data.first().contains(own))
            assertEquals(setOf("44"), store.data.first()[hidden])
        }

    @Test fun restoreClearsOptimisticHomeAndThemeOverrides() =
        runBlocking {
            val repository = DataStoreSettingsRepository(memoryStore())
            repository.setTheme(ThemePreference.AMOLED)
            repository.portable.replace(mapOf("theme" to "SYSTEM"), null)
            assertEquals(ThemePreference.SYSTEM, repository.theme.first())
        }

    @Test(expected = IllegalArgumentException::class)
    fun invalidNumericSettingsAreRejectedBeforeMutation() {
        DataStorePortableSettings(memoryStore()) {}.validate(mapOf("app_text_size" to "99999"), emptyMap())
    }

    @Test(expected = IllegalArgumentException::class)
    fun ownMarkerPayloadCannotWriteArbitraryPreferences() {
        DataStorePortableSettings(memoryStore()) {}.validate(emptyMap(), mapOf("pin" to "1234"))
    }

    @Test
    fun exportedSettingsUseCanonicalEffectiveValues() =
        runBlocking {
            val store = memoryStore(mutablePreferencesOf(intPreferencesKey("app_text_size") to 99999))
            val portable = DataStorePortableSettings(store) {}
            val values = portable.export()
            portable.validate(values, emptyMap())
            assertEquals(
                com.solthariel.pawchan.domain.settings.AppSettings.MAX_TEXT_SIZE
                    .toString(),
                values["app_text_size"],
            )
            assertEquals(99999, store.data.first()[intPreferencesKey("app_text_size")])
        }

    private fun memoryStore(initial: Preferences = emptyPreferences()): DataStore<Preferences> =
        object : DataStore<Preferences> {
            private val state = MutableStateFlow(initial)
            override val data = state

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                transform(state.value).also { state.value = it }
        }
}
