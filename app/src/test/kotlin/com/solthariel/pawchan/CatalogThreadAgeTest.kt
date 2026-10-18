package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.catalogThreadAge
import com.solthariel.pawchan.catalog.relativeThreadAge
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogThreadAgeTest {
    @Test
    fun missingOrInvalidTimeDoesNotInventAnAge() {
        assertEquals("", relativeThreadAge(null, NOW))
        assertEquals("", relativeThreadAge(0, NOW))
        assertEquals("", relativeThreadAge(-1, NOW))
    }

    @Test
    fun futureTimestampIsClampedWithoutNegativeAge() {
        assertEquals("только что", relativeThreadAge(NOW + 1, NOW))
        assertEquals("только что", relativeThreadAge(Long.MAX_VALUE, NOW))
    }

    @Test
    fun formatsMinutesHoursAndDaysAtBoundaries() {
        assertEquals("только что", relativeThreadAge(NOW - 59, NOW))
        assertEquals("1 мин назад", relativeThreadAge(NOW - 60, NOW))
        assertEquals("5 мин назад", relativeThreadAge(NOW - 300, NOW))
        assertEquals("1 ч назад", relativeThreadAge(NOW - 3600, NOW))
        assertEquals("2 ч назад", relativeThreadAge(NOW - 7200, NOW))
        assertEquals("3 дн назад", relativeThreadAge(NOW - 3 * DAY, NOW))
    }

    @Test
    fun formatsWeeksMonthsAndYears() {
        assertEquals("2 нед назад", relativeThreadAge(NOW - 14 * DAY, NOW))
        assertEquals("8 мес назад", relativeThreadAge(NOW - 240 * DAY, NOW))
        assertEquals("1 год назад", relativeThreadAge(NOW - YEAR, NOW))
        assertEquals("2 года назад", relativeThreadAge(NOW - 2 * YEAR, NOW))
        assertEquals("5 лет назад", relativeThreadAge(NOW - 5 * YEAR, NOW))
        assertEquals("11 лет назад", relativeThreadAge(NOW - 11 * YEAR, NOW))
        assertEquals("21 год назад", relativeThreadAge(NOW - 21 * YEAR, NOW))
    }

    @Test
    fun preparedAgeKeepsOriginalCreationTimeAndCanBeRefreshed() {
        val initial = catalogThreadAge(NOW - 60, NOW)
        assertEquals(NOW - 60, initial.createdAtEpochSeconds)
        assertEquals("1 мин назад", initial.text)
        assertEquals("2 мин назад", catalogThreadAge(initial.createdAtEpochSeconds, NOW + 60).text)
    }

    private companion object {
        const val NOW = 2_000_000_000L
        const val DAY = 86_400L
        const val YEAR = 365 * DAY
    }
}
