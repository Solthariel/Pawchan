package com.solthariel.pawchan.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PawchanDatabase::class.java,
        )

    @After
    fun deleteDatabase() {
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationOneToTwoPreservesLibraryAndCreatesWatchTable() {
        helper
            .createDatabase(TEST_DATABASE, 1)
            .apply {
                execSQL(
                    """
                    INSERT INTO thread_library (
                        site, board, threadId, subject, lastVisitedAtEpochMillis, favorite
                    ) VALUES ('dvach', 'b', $THREAD_ID, 'Сохранённый тред', $VISITED_AT, 1)
                    """.trimIndent(),
                )
                close()
            }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                2,
                true,
                MIGRATION_1_2,
            )
        migrated
            .query(
                """
                SELECT subject, lastVisitedAtEpochMillis, favorite
                FROM thread_library
                WHERE site = 'dvach' AND board = 'b' AND threadId = $THREAD_ID
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Сохранённый тред", cursor.getString(0))
                assertEquals(VISITED_AT, cursor.getLong(1))
                assertEquals(1, cursor.getInt(2))
            }
        migrated.query("SELECT COUNT(*) FROM thread_watch").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }
}

private const val TEST_DATABASE = "migration-test.db"
private const val THREAD_ID = 42L
private const val VISITED_AT = 100L
