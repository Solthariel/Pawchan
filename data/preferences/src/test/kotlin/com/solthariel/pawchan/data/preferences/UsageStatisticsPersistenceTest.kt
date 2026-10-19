package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.usageTotals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class UsageStatisticsPersistenceTest {
    @Test fun diskReopenKeepsObjectIdentityAndDailyMembership() =
        runBlocking {
            val directory = Files.createTempDirectory("usage-unique-test").toFile()
            val file = directory.resolve("usage.preferences_pb")
            val firstScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            val secondScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            try {
                val first = UsageStatisticsStore(PreferenceDataStoreFactory.create(scope = firstScope) { file }) { 1L }
                first.record(UsageMetric.THREADS_OPENED, identity = "thread")
                firstScope.coroutineContext[Job]?.cancelAndJoin()
                val secondStore = PreferenceDataStoreFactory.create(scope = secondScope) { file }
                val second = UsageStatisticsStore(secondStore) { 2L }
                second.record(UsageMetric.THREADS_OPENED, identity = "thread")
                assertEquals(1L, usageTotals(second.days.first())[UsageMetric.THREADS_OPENED])
                assertEquals(1L, usageTotals(second.days.first(), 2, 2)[UsageMetric.THREADS_OPENED])
            } finally {
                firstScope.coroutineContext[Job]?.cancelAndJoin()
                secondScope.coroutineContext[Job]?.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
}
