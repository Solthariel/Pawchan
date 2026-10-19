package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.domain.statistics.usageObjectHash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

class UsageStatisticsStore(
    private val store: DataStore<Preferences>,
    private val day: () -> Long = { LocalDate.now().toEpochDay() },
) : UsageStatisticsRepository {
    private val healthy = MutableStateFlow(true)
    override val available = healthy.asStateFlow()

    override val days =
        store.data
            .catch { error ->
                if (error !is IOException) throw error
                healthy.value = false
                emit(
                    androidx.datastore.preferences.core
                        .emptyPreferences(),
                )
            }.map(::usageSnapshot)

    override suspend fun record(
        metric: UsageMetric,
        amount: Long,
        identity: String?,
    ) {
        require(amount >= 0L)
        if (amount == 0L) return
        require(!identity.isNullOrBlank()) { "Statistics require a stable object identity" }
        require(metric == UsageMetric.BYTES_DOWNLOADED || amount == 1L) { "Record each distinct object separately" }
        val date = day()
        val hash = usageObjectHash(identity)
        safely {
            store.edit { values ->
                val key = stringSetPreferencesKey("$UNIQUE_USAGE_PREFIX$date|${metric.name}")
                val entries = values[key].orEmpty()
                val weights = usageObjectWeights(entries)
                val oldIds = values[stringSetPreferencesKey("usage_seen|$date|${metric.name}")].orEmpty()
                if (hash in oldIds || (weights[hash] ?: 0L) >= amount) return@edit
                values[key] = entries.filterNot { it.startsWith("$hash:") }.toSet() + "$hash:$amount"
            }
        }
    }

    override suspend fun reset() {
        safely {
            store.edit { values ->
                values
                    .asMap()
                    .keys
                    .filter {
                        it.name.startsWith("usage|") ||
                            it.name.startsWith("usage_seen|") ||
                            it.name.startsWith(UNIQUE_USAGE_PREFIX) ||
                            it.name == "usage_stickers_seen"
                    }.forEach { values.remove(it) }
            }
        }
    }

    private suspend fun safely(action: suspend () -> Unit) {
        try {
            action()
            healthy.value = true
        } catch (_: IOException) {
            healthy.value = false
        }
    }
}
