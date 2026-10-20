package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PostAppearanceMigrationTest {
    @get:Rule val temporary = TemporaryFolder()
    private val bubbles = booleanPreferencesKey("app_bubble_posts")
    private val theme = stringPreferencesKey("theme")

    @Test fun defaultAndExistingCardSettingsBecomeFrameless() =
        runBlocking {
            assertFalse(AppSettings().bubblePosts)
            val original = preferencesOf(bubbles to true, theme to "DARK")
            assertTrue(PostAppearanceMigration.shouldMigrate(original))
            val migrated = PostAppearanceMigration.migrate(original)
            assertFalse(requireNotNull(migrated[bubbles]))
            assertEquals("DARK", migrated[theme])
            assertTrue(requireNotNull(original[bubbles]))
            assertFalse(PostAppearanceMigration.shouldMigrate(migrated))
        }

    @Test fun manualChoiceSurvivesStoreReopeningAndMigrationDoesNotRunAgain() =
        runBlocking {
            val file = File(temporary.root, "appearance.preferences_pb")
            val oldJob = SupervisorJob()
            val old = PreferenceDataStoreFactory.create(scope = CoroutineScope(oldJob + Dispatchers.IO)) { file }
            old.edit {
                it[bubbles] = true
                it[theme] = "DARK"
            }
            oldJob.cancelAndJoin()
            val job = SupervisorJob()
            try {
                val migrated =
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(job + Dispatchers.IO),
                        migrations = listOf(PostAppearanceMigration),
                    ) { file }
                assertFalse(
                    migrated.data
                        .first()
                        .toAppSettings()
                        .bubblePosts,
                )
                migrated.edit { it[bubbles] = true }
            } finally {
                job.cancelAndJoin()
            }
            val nextJob = SupervisorJob()
            try {
                val reopened =
                    PreferenceDataStoreFactory.create(
                        scope = CoroutineScope(nextJob + Dispatchers.IO),
                        migrations = listOf(PostAppearanceMigration),
                    ) { file }
                val actual = reopened.data.first()
                assertTrue(actual.toAppSettings().bubblePosts)
                assertEquals("DARK", actual[theme])
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}
