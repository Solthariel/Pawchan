package com.solthariel.pawchan.data.preferences

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
            try {
                val old = diskPreferencesTestStore(file, CoroutineScope(oldJob + Dispatchers.IO))
                old.edit {
                    it[bubbles] = true
                    it[theme] = "DARK"
                }
            } finally {
                oldJob.cancelAndJoin()
            }
            val job = SupervisorJob()
            try {
                val migrated =
                    diskPreferencesTestStore(
                        file,
                        scope = CoroutineScope(job + Dispatchers.IO),
                        migrations = listOf(PostAppearanceMigration),
                    )
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
                    diskPreferencesTestStore(
                        file,
                        scope = CoroutineScope(nextJob + Dispatchers.IO),
                        migrations = listOf(PostAppearanceMigration),
                    )
                val actual = reopened.data.first()
                assertTrue(actual.toAppSettings().bubblePosts)
                assertEquals("DARK", actual[theme])
            } finally {
                nextJob.cancelAndJoin()
            }
        }
}
