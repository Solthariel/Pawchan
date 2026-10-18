package com.solthariel.pawchan.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable

/** AppStorage owns this repository; readers/writers only borrow it. All SQLite work stays on IO. */
class SqliteFilterStatisticsRepository(
    context: Context,
) : ContentFilterStatisticsRepository,
    Closeable {
    private val helper = FilterStatisticsDatabase(context.applicationContext)
    private val mutex = Mutex()
    private val closingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val version = MutableStateFlow(0L)
    private var closed = false
    private var writeFailed = false

    override val statistics =
        version.map {
            when (val result = access { db -> db.filterStatistics().copy(storageFailure = writeFailed) }) {
                is DomainOutcome.Success -> result.value
                is DomainOutcome.Failure -> ContentFilterStatistics(available = false, storageFailure = true)
            }
        }

    override suspend fun record(hits: List<ContentFilterHit>): DomainOutcome<Unit> {
        val result = access { db -> if (hits.isEmpty()) false else db.insertFilterHits(hits) }
        if (result is DomainOutcome.Failure || (result is DomainOutcome.Success && result.value)) {
            version.update { it + 1L }
        }
        return when (result) {
            is DomainOutcome.Success -> DomainOutcome.Success(Unit)
            is DomainOutcome.Failure -> result
        }
    }

    override suspend fun reset(): DomainOutcome<Unit> {
        val result =
            access { db ->
                db.delete("hits", null, null)
                writeFailed = false
            }
        version.update { it + 1L }
        return when (result) {
            is DomainOutcome.Success -> DomainOutcome.Success(Unit)
            is DomainOutcome.Failure -> result
        }
    }

    private suspend fun <T> access(operation: (SQLiteDatabase) -> T): DomainOutcome<T> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (closed) {
                    DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                } else {
                    try {
                        DomainOutcome.Success(operation(helper.writableDatabase))
                    } catch (_: SQLiteException) {
                        writeFailed = true
                        DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                    }
                }
            }
        }

    override fun close() {
        closingScope.launch {
            try {
                mutex.withLock {
                    closed = true
                    helper.close()
                }
            } finally {
                closingScope.cancel()
            }
        }
    }
}
