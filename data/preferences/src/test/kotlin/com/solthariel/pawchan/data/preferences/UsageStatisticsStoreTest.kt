package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.solthariel.pawchan.domain.statistics.UsageMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UsageStatisticsStoreTest {
    @Test fun countsOncePerIdentityAndKeepsDailyTotals() =
        runBlocking {
            val storage = MemoryStore()
            var day = 10L
            val stats = UsageStatisticsStore(storage) { day }
            stats.record(UsageMetric.POSTS_SENT, identity = "post")
            stats.record(UsageMetric.POSTS_SENT, identity = "post")
            stats.record(UsageMetric.POSTS_VIEWED, 7)
            assertEquals(
                1L,
                stats.days
                    .first()
                    .single()
                    .values[UsageMetric.POSTS_SENT],
            )
            assertEquals(
                7L,
                stats.days
                    .first()
                    .single()
                    .values[UsageMetric.POSTS_VIEWED],
            )
            day = 11L
            stats.record(UsageMetric.POSTS_VIEWED, 3)
            assertEquals(10L, stats.days.first().sumOf { it.values[UsageMetric.POSTS_VIEWED] ?: 0L })
            val reopened = UsageStatisticsStore(storage) { day }
            assertEquals(stats.days.first(), reopened.days.first())
        }

    @Test fun resetClearsCountersAndDeduplication() =
        runBlocking {
            val stats = UsageStatisticsStore(MemoryStore()) { 1L }
            stats.record(UsageMetric.FILES_DOWNLOADED, identity = "file")
            stats.reset()
            assertTrue(stats.days.first().isEmpty())
            stats.record(UsageMetric.FILES_DOWNLOADED, identity = "file")
            assertEquals(
                1L,
                stats.days
                    .first()
                    .single()
                    .values[UsageMetric.FILES_DOWNLOADED],
            )
        }

    @Test fun overflowSaturatesAndZeroCreatesNoEntry() =
        runBlocking {
            val stats = UsageStatisticsStore(MemoryStore()) { 1L }
            stats.record(UsageMetric.BYTES_DOWNLOADED, 0)
            assertTrue(stats.days.first().isEmpty())
            stats.record(UsageMetric.BYTES_DOWNLOADED, Long.MAX_VALUE)
            stats.record(UsageMetric.BYTES_DOWNLOADED, 5)
            assertEquals(
                Long.MAX_VALUE,
                stats.days
                    .first()
                    .single()
                    .values[UsageMetric.BYTES_DOWNLOADED],
            )
        }

    @Test fun writeFailureDoesNotBreakTheUserActionAndIsReported() =
        runBlocking {
            val storage = MemoryStore()
            val stats = UsageStatisticsStore(storage)
            storage.fail = true
            stats.record(UsageMetric.POSTS_SENT)
            assertFalse(stats.available.first())
            storage.fail = false
            stats.record(UsageMetric.POSTS_SENT)
            assertTrue(stats.available.first())
        }

    @Test fun hiddenStickerIsCountedOnceAcrossReopenDaysAndSettingToggles() =
        runBlocking {
            val storage = MemoryStore()
            var day = 1L
            val first = UsageStatisticsStore(storage) { day }
            first.record(UsageMetric.STICKERS_HIDDEN, identity = "sticker-url")
            first.record(UsageMetric.STICKERS_HIDDEN, identity = "sticker-url")
            day = 2L
            val reopened = UsageStatisticsStore(storage) { day }
            reopened.record(UsageMetric.STICKERS_HIDDEN, identity = "sticker-url")
            reopened.record(UsageMetric.STICKERS_HIDDEN, identity = "another-url")
            assertEquals(2L, reopened.days.first().sumOf { it.values[UsageMetric.STICKERS_HIDDEN] ?: 0L })
            reopened.reset()
            reopened.record(UsageMetric.STICKERS_HIDDEN, identity = "sticker-url")
            assertEquals(1L, reopened.days.first().sumOf { it.values[UsageMetric.STICKERS_HIDDEN] ?: 0L })
        }

    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        var fail = false
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            if (fail) throw IOException("unavailable")
            return transform(state.value).also { state.value = it }
        }
    }
}
