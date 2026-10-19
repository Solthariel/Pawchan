package com.solthariel.pawchan.domain.statistics

/** Distinct objects across the selected days, plus legacy totals whose identities were not retained. */
fun usageTotals(
    days: List<UsageDay>,
    from: Long = Long.MIN_VALUE,
    through: Long = Long.MAX_VALUE,
): Map<UsageMetric, Long> =
    UsageMetric.entries.associateWith { metric ->
        var legacy = 0L
        val objects = mutableMapOf<String, Long>()
        days.filter { it.epochDay in from..through }.forEach { day ->
            val identified = day.objects[metric].orEmpty()
            val known = identified.values.fold(0L, ::addUsageValues)
            val unknown = ((day.values[metric] ?: 0L) - known).coerceAtLeast(0L)
            legacy = addUsageValues(legacy, unknown)
            identified.forEach { (id, weight) -> objects[id] = maxOf(objects[id] ?: 0L, weight) }
        }
        objects.values.fold(legacy, ::addUsageValues)
    }

fun addUsageValues(
    first: Long,
    second: Long,
): Long = if (Long.MAX_VALUE - first < second) Long.MAX_VALUE else first + second

/** Aggregates without retained IDs cannot be retroactively made unique. */
fun UsageDay.hasLegacyUsage(): Boolean =
    values.any { (metric, amount) -> amount > objects[metric].orEmpty().values.fold(0L, ::addUsageValues) }
