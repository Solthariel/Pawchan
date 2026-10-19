package com.solthariel.pawchan.domain.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTotalsTest {
    private val metric = UsageMetric.THREADS_OPENED

    @Test fun periodsUnionObjectsInsteadOfAddingDailyCounts() {
        val days = listOf(day(1, "a"), day(2, "a", "b"), day(40, "a"))
        assertEquals(2L, usageTotals(days, 2, 2)[metric])
        assertEquals(2L, usageTotals(days, 1, 2)[metric])
        assertEquals(2L, usageTotals(days)[metric])
        assertEquals(1L, usageTotals(days, 40, 40)[metric])
    }

    @Test fun oldAnonymousTotalsArePreservedWithoutInventingIdentities() {
        val days = listOf(UsageDay(1, mapOf(metric to 5L)), day(2, "a"), day(3, "a"))
        assertEquals(6L, usageTotals(days)[metric])
    }

    @Test fun mixedLegacyAndIdentifiedValuesAreNotCountedTwice() {
        val days = listOf(UsageDay(1, mapOf(metric to 6L), mapOf(metric to mapOf("a" to 1L))), day(2, "a"))
        assertEquals(6L, usageTotals(days)[metric])
    }

    @Test fun byteVolumeUsesEachFileOnceAndDoesNotOverflow() {
        val bytes = UsageMetric.BYTES_DOWNLOADED
        val days =
            listOf(
                UsageDay(1, mapOf(bytes to 10L), mapOf(bytes to mapOf("file" to 10L))),
                UsageDay(2, mapOf(bytes to 12L), mapOf(bytes to mapOf("file" to 12L))),
            )
        assertEquals(12L, usageTotals(days)[bytes])
        assertEquals(Long.MAX_VALUE, addUsageValues(Long.MAX_VALUE, 1L))
    }

    @Test fun legacyNoticeIsNeededOnlyForUnidentifiedTotals() {
        assertEquals(true, UsageDay(1, mapOf(metric to 5L)).hasLegacyUsage())
        assertEquals(false, day(1, "a").hasLegacyUsage())
    }

    @Test fun postCountUsesTheSameUniqueTotals() {
        val text = appendPostCount("/postcount", listOf(day(1, "a"), day(2, "a")))
        assertEquals(true, text.contains("Открыто тредов: 1"))
    }

    private fun day(
        date: Long,
        vararg ids: String,
    ): UsageDay = UsageDay(date, mapOf(metric to ids.size.toLong()), mapOf(metric to ids.associateWith { 1L }))
}
