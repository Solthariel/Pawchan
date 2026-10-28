package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Base64

class FilterGroupsResetTest {
    @get:Rule val temp = TemporaryFolder()
    private val current =
        ContentFilterRule(
            "current",
            "dvach",
            targets = ContentFilterTargets(comments = false),
            condition = ContentFilterConditionSpec(ContentFilterMode.ALL_TERMS, listOf("пустите", "фап, fap")),
        )

    @Test fun fullPreviousRecordIsRejectedButCurrentGroupsAndScopeRoundTrip() {
        assertEquals(emptyList<ContentFilterRule>(), decodeContentFilters(previousRecord()))
        assertEquals(listOf(current), decodeContentFilters(encodeContentFilters(listOf(current))))
    }

    @Test fun resetRemovesOnlyUnsupportedRulesAndPreservesNoticeAndUnrelatedSettings() =
        runTest {
            val theme = stringPreferencesKey("theme")
            val original =
                mutablePreferencesOf(
                    CONTENT_FILTER_RECORDS to (previousRecord() + "\n" + encodeContentFilters(listOf(current))),
                    theme to "DARK",
                )
            assertTrue(ContentFilterResetMigration.shouldMigrate(original))
            val next = ContentFilterResetMigration.migrate(original)
            assertEquals(listOf(current), decodeContentFilters(next[CONTENT_FILTER_RECORDS].orEmpty()))
            assertEquals("DARK", next[theme])
            assertTrue(next.toAppSettings().behavior.contentFilterResetNotice)
            assertFalse(ContentFilterResetMigration.shouldMigrate(next))
            assertEquals(next, ContentFilterResetMigration.migrate(next))
            val dismissed =
                next.toMutablePreferences().apply {
                    val app = toAppSettings()
                    val behavior = app.behavior.copy(autohide = app.behavior.autohide.copy(resetNotice = false))
                    write(app.copy(behavior = behavior))
                }
            assertFalse(dismissed.toAppSettings().behavior.contentFilterResetNotice)
            assertFalse(ContentFilterResetMigration.shouldMigrate(dismissed))
        }

    @Test fun diskReopeningDoesNotResetNewRulesOrShowDismissedNoticeAgain() =
        runTest {
            val file = temp.newFolder("groups").resolve("rules.preferences_pb")

            suspend fun <T> open(action: suspend (DataStore<Preferences>) -> T): T {
                val job = SupervisorJob()
                val store =
                    diskPreferencesTestStore(
                        file,
                        CoroutineScope(job + Dispatchers.IO),
                        listOf(ContentFilterResetMigration),
                    )
                return try {
                    action(store)
                } finally {
                    job.cancelAndJoin()
                }
            }
            open { store -> store.edit { it[CONTENT_FILTER_RECORDS] = previousRecord() } }
            open { store ->
                assertTrue(
                    store.data
                        .first()
                        .toAppSettings()
                        .behavior.contentFilterResetNotice,
                )
                assertEquals(
                    emptyList<ContentFilterRule>(),
                    store.data
                        .first()
                        .toAppSettings()
                        .contentFilters,
                )
                store.edit {
                    it[CONTENT_FILTER_RECORDS] = encodeContentFilters(listOf(current))
                    it[CONTENT_FILTER_RESET_NOTICE] = false
                }
            }
            open { store ->
                val settings = store.data.first().toAppSettings()
                assertEquals(listOf(current), settings.contentFilters)
                assertFalse(settings.behavior.contentFilterResetNotice)
            }
        }

    @Test fun freshInstallDoesNotShowResetNotice() =
        runTest {
            val fresh = mutablePreferencesOf()
            assertFalse(ContentFilterResetMigration.shouldMigrate(fresh))
            assertFalse(
                ContentFilterResetMigration
                    .migrate(fresh)
                    .toAppSettings()
                    .behavior.contentFilterResetNotice,
            )
        }

    @Test fun portableImportDiscardsPreviousRulesImmediatelyWithoutReinterpretingCommas() {
        val incoming = mapOf("app_content_filters_v1" to previousRecord())
        val restored = validatePortablePreferences(incoming)
        assertEquals("", restored[CONTENT_FILTER_RECORDS])
        assertTrue(restored.toAppSettings().behavior.contentFilterResetNotice)
    }

    private fun previousRecord(): String {
        val encoded = encodeContentFilters(listOf(current)).split('\t').toMutableList()
        encoded[0] = Base64.getEncoder().encodeToString("5".toByteArray())
        return encoded.joinToString("\t")
    }
}
