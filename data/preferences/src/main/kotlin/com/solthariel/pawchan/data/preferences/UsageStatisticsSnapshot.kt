package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.Preferences
import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.addUsageValues

internal const val UNIQUE_USAGE_PREFIX = "usage_unique|"

internal fun usageObjectWeights(entries: Set<String>): Map<String, Long> =
    entries
        .mapNotNull { entry ->
            val parts = entry.split(':', limit = 2)
            val weight = parts.getOrNull(1)?.toLongOrNull()
            if (parts[0].length == USAGE_HASH_LENGTH && weight != null && weight > 0L) parts[0] to weight else null
        }.groupBy({ it.first }, { it.second })
        .mapValues { (_, weights) -> weights.max() }

/** Old aggregate values remain readable; retained old per-day IDs recover a known subset. */
internal fun usageSnapshot(preferences: Preferences): List<UsageDay> {
    val totals = mutableMapOf<Pair<Long, UsageMetric>, Long>()
    val oldIds = mutableMapOf<Pair<Long, UsageMetric>, Set<String>>()
    val objects = mutableMapOf<Pair<Long, UsageMetric>, Map<String, Long>>()
    preferences.asMap().forEach { (key, value) ->
        val parts = key.name.split('|')
        if (parts.size != USAGE_KEY_PARTS) return@forEach
        val date = parts[1].toLongOrNull() ?: return@forEach
        val metric = UsageMetric.entries.firstOrNull { it.name == parts[2] } ?: return@forEach
        val entry = date to metric
        when (parts[0]) {
            "usage" -> {
                (value as? Long)?.let { totals[entry] = it.coerceAtLeast(0L) }
            }

            "usage_seen" -> {
                oldIds[entry] = (value as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            }

            "usage_unique" -> {
                val entries = (value as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
                objects[entry] = usageObjectWeights(entries)
            }
        }
    }
    val keys = totals.keys + objects.keys
    return keys.map { it.first }.distinct().sorted().map { date ->
        val values = mutableMapOf<UsageMetric, Long>()
        val identities = mutableMapOf<UsageMetric, Map<String, Long>>()
        keys.filter { it.first == date }.forEach { entry ->
            val metric = entry.second
            val old = totals[entry] ?: 0L
            val retained =
                if (metric ==
                    UsageMetric.BYTES_DOWNLOADED
                ) {
                    emptySet()
                } else {
                    oldIds[entry].orEmpty().take(old.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).toSet()
                }
            val known = retained.associateWith { 1L }.toMutableMap()
            objects[entry].orEmpty().forEach { (id, weight) -> known[id] = maxOf(known[id] ?: 0L, weight) }
            val legacy = old - retained.size
            values[metric] = known.values.fold(legacy, ::addUsageValues)
            if (known.isNotEmpty()) identities[metric] = known
        }
        UsageDay(date, values, identities)
    }
}

private const val USAGE_HASH_LENGTH = 64
private const val USAGE_KEY_PARTS = 3
