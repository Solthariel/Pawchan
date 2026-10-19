package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.usageTotals
import java.time.LocalDate

internal fun usagePeriodTotals(
    days: List<UsageDay>,
    period: Int,
    today: LocalDate,
): Map<UsageMetric, Long> {
    val lower =
        when (period) {
            0 -> today.toEpochDay()
            1 -> today.withDayOfMonth(1).toEpochDay()
            else -> Long.MIN_VALUE
        }
    return usageTotals(days, lower, today.toEpochDay())
}
