package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
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
            }.map { preferences ->
                preferences
                    .asMap()
                    .entries
                    .mapNotNull { (key, value) ->
                        val parts = key.name.split('|')
                        if (parts.size != PARTS || parts[0] != "usage") return@mapNotNull null
                        val epochDay = parts[1].toLongOrNull() ?: return@mapNotNull null
                        val metric = UsageMetric.entries.firstOrNull { it.name == parts[2] } ?: return@mapNotNull null
                        (value as? Long)?.let { Triple(epochDay, metric, it) }
                    }.groupBy { it.first }
                    .map { (date, rows) -> UsageDay(date, rows.associate { it.second to it.third }) }
            }

    override suspend fun record(
        metric: UsageMetric,
        amount: Long,
        identity: String?,
    ) {
        require(amount >= 0L)
        if (amount == 0L) return
        val date = day()
        safely {
            store.edit { values ->
                val seenKey =
                    stringSetPreferencesKey(
                        if (metric == UsageMetric.STICKERS_HIDDEN) {
                            "usage_stickers_seen"
                        } else {
                            "usage_seen|$date|${metric.name}"
                        },
                    )
                val hash =
                    identity?.let { value ->
                        MessageDigest
                            .getInstance("SHA-256")
                            .digest(value.toByteArray())
                            .joinToString("") { "%02x".format(it) }
                    }
                if (hash != null && hash in values[seenKey].orEmpty()) return@edit
                val key = longPreferencesKey("usage|$date|${metric.name}")
                val old = values[key] ?: 0L
                values[key] = if (Long.MAX_VALUE - old < amount) Long.MAX_VALUE else old + amount
                if (hash != null) values[seenKey] = values[seenKey].orEmpty() + hash
                values
                    .asMap()
                    .keys
                    .filter {
                        it.name.startsWith("usage_seen|") &&
                            !it.name.startsWith("usage_seen|$date|")
                    }.forEach { values.remove(it) }
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

private const val PARTS = 3
