package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.usageLabel as domainUsageLabel

internal fun UsageMetric.usageLabel(): String = domainUsageLabel()

internal fun usageValue(
    metric: UsageMetric,
    value: Long,
): String =
    if (metric == UsageMetric.BYTES_DOWNLOADED) {
        com.solthariel.pawchan.core.ui
            .formatByteSizeForUi(value)
    } else {
        value.toString()
    }
