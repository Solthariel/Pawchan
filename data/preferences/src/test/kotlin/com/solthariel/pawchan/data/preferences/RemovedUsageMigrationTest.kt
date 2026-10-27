package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.statistics.UsageMetric
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

class RemovedUsageMigrationTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun removesAllStoredFormsButPreservesOtherCountersAndReadingPosition() =
        runBlocking {
            val old =
                emptyPreferences().toMutablePreferences().apply {
                    this[longPreferencesKey("usage|1|POSTS_VIEWED")] = 500L
                    this[stringSetPreferencesKey("usage_seen|1|POSTS_VIEWED")] = setOf("post")
                    this[stringSetPreferencesKey("usage_unique|2|POSTS_VIEWED")] = setOf("post:1")
                    this[longPreferencesKey("usage|1|POSTS_SENT")] = 3L
                    this[longPreferencesKey("reading_position")] = 42L
                }
            assertTrue(RemovedUsageMigration.shouldMigrate(old))
            val migrated = RemovedUsageMigration.migrate(old)
            assertFalse(RemovedUsageMigration.shouldMigrate(migrated))
            assertEquals(2, migrated.asMap().size)
            assertEquals(3L, migrated[longPreferencesKey("usage|1|POSTS_SENT")])
            assertEquals(42L, migrated[longPreferencesKey("reading_position")])
            assertEquals(migrated, RemovedUsageMigration.migrate(migrated))
            assertFalse(UsageMetric.entries.any { it.name == "POSTS_VIEWED" })
        }

    @Test fun migrationIsPersistedOnDiskAndDoesNotResetRemainingStatistics() =
        runBlocking {
            val file = File(temporary.root, "usage.preferences_pb")
            val oldJob = SupervisorJob()
            try {
                val old = diskPreferencesTestStore(file, CoroutineScope(oldJob + Dispatchers.IO))
                old.edit {
                    it[longPreferencesKey("usage|1|POSTS_VIEWED")] = 500L
                    it[longPreferencesKey("usage|1|POSTS_SENT")] = 3L
                }
            } finally {
                oldJob.cancelAndJoin()
            }
            repeat(2) {
                val job = SupervisorJob()
                try {
                    val store =
                        diskPreferencesTestStore(
                            file,
                            scope = CoroutineScope(job + Dispatchers.IO),
                            migrations = listOf(RemovedUsageMigration),
                        )
                    val actual = store.data.first()
                    assertFalse(actual.asMap().keys.any { it.name.endsWith("|POSTS_VIEWED") })
                    assertEquals(3L, actual[longPreferencesKey("usage|1|POSTS_SENT")])
                } finally {
                    job.cancelAndJoin()
                }
            }
        }

    @Test fun legacyOnlyDaysDoNotAppearInStatisticsEvenBeforeMigration() {
        val old = emptyPreferences().toMutablePreferences()
        old[longPreferencesKey("usage|1|POSTS_VIEWED")] = 500L
        assertTrue(usageSnapshot(old).isEmpty())
    }
}
