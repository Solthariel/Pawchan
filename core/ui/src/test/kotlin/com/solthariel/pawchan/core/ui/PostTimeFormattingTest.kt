package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class PostTimeFormattingTest {
    private val utc = PostDisplayPreferences(zone = ZoneOffset.UTC)

    @Test fun everyPostIncludesYear() {
        assertEquals("11.09.26 20:56:34", formatPostTime(Instant.parse("2026-09-11T20:56:34Z").epochSecond, utc))
        assertEquals("05.03.25 13:02:51", formatPostTime(Instant.parse("2025-03-05T13:02:51Z").epochSecond, utc))
    }

    @Test fun timezoneUsesProvidedDeviceZone() {
        val local = utc.copy(zone = ZoneId.of("Asia/Yekaterinburg"))
        assertEquals("11.01.26 02:30:00", formatPostTime(Instant.parse("2026-01-10T21:30:00Z").epochSecond, local))
    }
}
