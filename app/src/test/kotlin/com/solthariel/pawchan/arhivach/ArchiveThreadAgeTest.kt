package com.solthariel.pawchan.arhivach

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ArchiveThreadAgeTest {
    private val zone = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 11, 15, 12, 0).atZone(zone).toEpochSecond()

    @Test
    fun formatsArchiveDateLikeOtherThreadLists() {
        assertEquals("5 мин назад", formatArchiveThreadAge("2026-11-15 11:55:00", now, zone))
        assertEquals("1 ч назад", formatArchiveThreadAge("2026-11-15 11:00", now, zone))
    }

    @Test
    fun keepsOriginalTextWhenArchiveDateCannotBeParsed() {
        assertEquals("неизвестная дата", formatArchiveThreadAge("неизвестная дата", now, zone))
    }

    @Test
    fun keepsEmptyDateEmpty() {
        assertEquals("", formatArchiveThreadAge(null, now, zone))
        assertEquals("", formatArchiveThreadAge("  ", now, zone))
    }
}
