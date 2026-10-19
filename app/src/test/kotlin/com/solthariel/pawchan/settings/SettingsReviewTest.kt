package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.statistics.UsageDay
import com.solthariel.pawchan.domain.statistics.UsageMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SettingsReviewTest {
    @Test fun directoryLabelHidesSystemUriAndPreservesSpacesAndPlus() {
        assertEquals("Выбрать папку", downloadDirectoryLabel(null))
        assertEquals(
            "Download/Pawchan",
            downloadDirectoryLabel(
                "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FPawchan",
            ),
        )
        assertEquals("A+B C", downloadDirectoryLabel("content://provider/tree/primary%3AA%2BB%20C"))
        assertEquals("Выбранная папка", downloadDirectoryLabel("content://provider/tree/%invalid"))
    }

    @Test fun cacheSliderCoversDecimalRangeAndIsMonotonic() {
        assertEquals(100, cacheSliderSize(0f))
        assertEquals(10000, cacheSliderSize(1f))
        assertEquals(5000, cacheSliderSize(0.5f))
        assertEquals(2500, cacheSliderSize(0.25f))
        val sizes = (0..100).map { cacheSliderSize(it / 100f) }
        assertTrue(sizes.zipWithNext().all { (first, second) -> first <= second })
        listOf(100, 500, 1000, 5000, 10000).forEach { size ->
            assertTrue(kotlin.math.abs(size - cacheSliderSize(cacheSliderPosition(size))) <= 1)
        }
    }

    @Test fun periodsDeduplicateRepeatedObjectsAcrossDays() {
        val today = LocalDate.of(2026, 10, 19)
        val metric = UsageMetric.THREADS_OPENED
        val days =
            listOf(
                UsageDay(today.minusDays(1).toEpochDay(), mapOf(metric to 1L), mapOf(metric to mapOf("a" to 1L))),
                UsageDay(today.toEpochDay(), mapOf(metric to 2L), mapOf(metric to mapOf("a" to 1L, "b" to 1L))),
            )
        assertEquals(2L, usagePeriodTotals(days, 0, today)[metric])
        assertEquals(2L, usagePeriodTotals(days, 1, today)[metric])
        assertEquals(2L, usagePeriodTotals(days, 2, today)[metric])
    }

    @Test fun periodsExcludeOtherDaysAndFutureEntries() {
        val today = LocalDate.of(2026, 10, 16)
        val metric = UsageMetric.POSTS_SENT
        val days =
            listOf(
                UsageDay(today.toEpochDay(), mapOf(metric to 2L)),
                UsageDay(today.minusDays(1).toEpochDay(), mapOf(metric to 3L)),
                UsageDay(today.minusMonths(1).toEpochDay(), mapOf(metric to 5L)),
                UsageDay(today.plusDays(1).toEpochDay(), mapOf(metric to 7L)),
            )
        assertEquals(2L, usagePeriodTotals(days, 0, today)[metric])
        assertEquals(5L, usagePeriodTotals(days, 1, today)[metric])
        assertEquals(10L, usagePeriodTotals(days, 2, today)[metric])
    }
}
