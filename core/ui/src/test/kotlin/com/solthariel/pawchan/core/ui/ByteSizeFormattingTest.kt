package com.solthariel.pawchan.core.ui

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Locale

class ByteSizeFormattingTest {
    @Test
    fun bytesAndUnitBoundariesUseShortLabels() {
        val cases =
            listOf(
                0L to "0 Б",
                1L to "1 Б",
                999L to "999 Б",
                1_000L to "1.0 КБ",
                1_500L to "1.5 КБ",
                10_000L to "10 КБ",
                475_000L to "475 КБ",
                1_000_000L to "1.0 МБ",
                1_000_000_000L to "1.0 ГБ",
                1_000_000_000_000L to "1.0 ТБ",
            )
        for ((bytes, expected) in cases) assertEquals(expected, formatByteSizeForUi(bytes))
    }

    @Test
    fun decimalCacheSizesMatchTheConfiguredLimit() {
        assertEquals("5.0 ГБ", formatByteSizeForUi(5_000_000_000L))
        assertEquals("30 МБ", formatByteSizeForUi(30_000_000L))
        assertEquals("55 КБ", formatByteSizeForUi(55_000L))
    }

    @Test
    fun decimalMegabytesAreNotMebibytes() {
        assertEquals("2.0 МБ", formatByteSizeForUi(2_000_000L))
        assertEquals("2.1 МБ", formatByteSizeForUi(2L * 1024 * 1024))
    }

    @Test
    fun labelsNeverContainTheBinaryUnitLetter() {
        for (size in listOf(1_000L, 1_000_000L, 1_000_000_000L, 1_000_000_000_000L)) {
            assertFalse(formatByteSizeForUi(size).contains('и', ignoreCase = true))
        }
    }

    @Test
    fun formattingDoesNotDependOnTheDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.5 КБ", formatByteSizeForUi(1_500L))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
