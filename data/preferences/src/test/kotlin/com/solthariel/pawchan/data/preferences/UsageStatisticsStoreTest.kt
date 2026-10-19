package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.usageObjectHash
import com.solthariel.pawchan.domain.statistics.usageTotals
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UsageStatisticsStoreTest {
    @Test fun everyMetricIsUniqueAcrossRepeatRestartAndSelectedPeriods() =
        runBlocking {
            UsageMetric.entries.forEach { metric ->
                val storage = MemoryStore()
                var day = 10L
                val weight = if (metric == UsageMetric.BYTES_DOWNLOADED) 12L else 1L
                val stats = UsageStatisticsStore(storage) { day }
                repeat(3) { stats.record(metric, weight, "object") }
                UsageStatisticsStore(storage) { day }.record(metric, weight, "object")
                assertEquals(weight, usageTotals(stats.days.first())[metric])
                day = 11L
                val reopened = UsageStatisticsStore(storage) { day }
                reopened.record(metric, weight, "object")
                reopened.record(metric, weight, "other")
                assertEquals(weight * 2, usageTotals(reopened.days.first(), 11, 11)[metric])
                assertEquals(weight * 2, usageTotals(reopened.days.first())[metric])
            }
        }

    @Test fun concurrentRepeatedRecordsAreAtomic() =
        runBlocking {
            val stats = UsageStatisticsStore(MemoryStore()) { 1L }
            coroutineScope {
                repeat(100) { launch { stats.record(UsageMetric.THREADS_OPENED, identity = "same") } }
            }
            assertEquals(1L, usageTotals(stats.days.first())[UsageMetric.THREADS_OPENED])
        }

    @Test fun resetClearsLegacyTotalsAndNewObjectIdentities() =
        runBlocking {
            val storage = MemoryStore()
            storage.edit { it[longPreferencesKey("usage|1|FILES_DOWNLOADED")] = 7L }
            val stats = UsageStatisticsStore(storage) { 1L }
            stats.record(UsageMetric.FILES_DOWNLOADED, identity = "file")
            stats.reset()
            assertTrue(stats.days.first().isEmpty())
            stats.record(UsageMetric.FILES_DOWNLOADED, identity = "file")
            assertEquals(1L, usageTotals(stats.days.first())[UsageMetric.FILES_DOWNLOADED])
        }

    @Test fun overflowSaturatesAndZeroCreatesNoEntry() =
        runBlocking {
            val stats = UsageStatisticsStore(MemoryStore()) { 1L }
            stats.record(UsageMetric.BYTES_DOWNLOADED, 0)
            assertTrue(stats.days.first().isEmpty())
            stats.record(UsageMetric.BYTES_DOWNLOADED, Long.MAX_VALUE, "first")
            stats.record(UsageMetric.BYTES_DOWNLOADED, 5, "second")
            assertEquals(Long.MAX_VALUE, usageTotals(stats.days.first())[UsageMetric.BYTES_DOWNLOADED])
        }

    @Test fun writeFailureIsReportedAndTheSameObjectCanBeRetried() =
        runBlocking {
            val storage = MemoryStore()
            val stats = UsageStatisticsStore(storage) { 1L }
            storage.fail = true
            stats.record(UsageMetric.POSTS_SENT, identity = "post")
            assertFalse(stats.available.first())
            storage.fail = false
            stats.record(UsageMetric.POSTS_SENT, identity = "post")
            assertTrue(stats.available.first())
            assertEquals(1L, usageTotals(stats.days.first())[UsageMetric.POSTS_SENT])
        }

    @Test fun legacyAggregatesArePreservedAndRetainedIdsAreNotCountedAgain() =
        runBlocking {
            val storage = MemoryStore()
            val metric = UsageMetric.POSTS_SENT
            storage.edit {
                it[longPreferencesKey("usage|1|POSTS_SENT")] = 5L
                it[stringSetPreferencesKey("usage_seen|1|POSTS_SENT")] = setOf(usageObjectHash("known"))
            }
            var day = 1L
            val stats = UsageStatisticsStore(storage) { day }
            stats.record(metric, identity = "known")
            assertEquals(5L, usageTotals(stats.days.first())[metric])
            stats.record(metric, identity = "new")
            assertEquals(6L, usageTotals(stats.days.first())[metric])
            day = 2L
            stats.record(metric, identity = "known")
            stats.record(metric, identity = "new")
            assertEquals(6L, usageTotals(stats.days.first())[metric])
            assertEquals(2L, usageTotals(stats.days.first(), 2, 2)[metric])
        }

    @Test fun retainedDailyIdsAreNotPrunedOnTheNextDay() =
        runBlocking {
            val storage = MemoryStore()
            var day = 1L
            val stats = UsageStatisticsStore(storage) { day }
            stats.record(UsageMetric.IMAGES_OPENED, identity = "image")
            day = 2L
            stats.record(UsageMetric.IMAGES_OPENED, identity = "image")
            val keys =
                storage.data
                    .first()
                    .asMap()
                    .keys
                    .map { it.name }
            assertTrue("usage_unique|1|IMAGES_OPENED" in keys)
            assertTrue("usage_unique|2|IMAGES_OPENED" in keys)
        }

    @Test(expected = IllegalArgumentException::class)
    fun missingIdentityCannotSilentlyReintroduceEventCounting() =
        runBlocking { UsageStatisticsStore(MemoryStore()).record(UsageMetric.THREADS_OPENED) }

    @Test(expected = IllegalArgumentException::class)
    fun batchCountersCannotPretendToRepresentOneObject() =
        runBlocking { UsageStatisticsStore(MemoryStore()).record(UsageMetric.POSTS_VIEWED, 7, "post") }

    private class MemoryStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        var fail = false
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock {
                if (fail) throw IOException("unavailable")
                transform(state.value).also { state.value = it }
            }
    }
}
