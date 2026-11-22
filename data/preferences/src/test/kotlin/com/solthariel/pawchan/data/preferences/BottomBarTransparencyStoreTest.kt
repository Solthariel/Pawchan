package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BottomBarTransparencyStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun missingKeyDefaultsToTwentyPercent() {
        assertEquals(20, emptyPreferences().readNavigation().bottomBarTransparencyPercent)
    }

    @Test fun storedBoundariesAndInvalidValuesAreNormalizedWithoutResettingOtherLimits() {
        val key = intPreferencesKey("navigation_bottom_bar_transparency_percent")
        listOf(-1 to 0, 0 to 0, 20 to 20, 100 to 100, 101 to 100).forEach { (stored, expected) ->
            val preferences = mutablePreferencesOf(key to stored, intPreferencesKey("navigation_pages_limit") to 9)
            val loaded = preferences.readNavigation()
            assertEquals(expected, loaded.bottomBarTransparencyPercent)
            assertEquals(9, loaded.pages)
            preferences.writeNavigation(loaded.copy(favorites = 7))
            assertEquals(expected, preferences.readNavigation().bottomBarTransparencyPercent)
            assertEquals(7, preferences.readNavigation().favorites)
        }
    }

    @Test fun topBarDefaultsClampsAndPersistsIndependently() {
        val key = intPreferencesKey("navigation_top_bar_transparency_percent")
        assertEquals(0, emptyPreferences().readNavigation().topBarTransparencyPercent)
        listOf(-1 to 0, 0 to 0, 37 to 37, 100 to 100, 101 to 100).forEach { (stored, expected) ->
            val preferences = mutablePreferencesOf(key to stored)
            val loaded = preferences.readNavigation()
            assertEquals(expected, loaded.topBarTransparencyPercent)
            preferences.writeNavigation(loaded.copy(bottomBarTransparencyPercent = 55))
            assertEquals(expected, preferences.readNavigation().topBarTransparencyPercent)
            assertEquals(55, preferences.readNavigation().bottomBarTransparencyPercent)
        }
    }

    @Test fun changedValueSurvivesARealDataStoreReopen() =
        runBlocking {
            val file = File(temporary.root, "bottom-bar.preferences_pb")
            val firstJob = SupervisorJob()
            try {
                val store =
                    AppSettingsStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(firstJob + Dispatchers.IO)) { file },
                    )
                val outcome =
                    store.updateAppSettings { app ->
                        val client = app.behavior.client
                        val navigation =
                            client.reading.navigation.copy(
                                bottomBarTransparencyPercent = 37,
                                topBarTransparencyPercent = 61,
                            )
                        app.copy(
                            behavior =
                                app.behavior.copy(
                                    client =
                                        client.copy(
                                            reading =
                                                client.reading.copy(
                                                    navigation = navigation,
                                                ),
                                        ),
                                ),
                        )
                    }
                assertEquals(DomainOutcome.Success(Unit), outcome)
            } finally {
                firstJob.cancelAndJoin()
            }
            val nextJob = SupervisorJob()
            try {
                val restored =
                    AppSettingsStore(
                        PreferenceDataStoreFactory.create(scope = CoroutineScope(nextJob + Dispatchers.IO)) { file },
                    ).appSettings.first()
                assertEquals(37, restored.behavior.client.reading.navigation.bottomBarTransparencyPercent)
                assertEquals(61, restored.behavior.client.reading.navigation.topBarTransparencyPercent)
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}
