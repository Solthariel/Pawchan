package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ContentFilterStatisticsFormatTest {
    @Test fun timestampHasCompactFixedFormatAndUsesDeviceZone() {
        val time = Instant.parse("2026-10-19T20:38:14Z").toEpochMilli()
        assertEquals("19.10.2026 23:38:14", formatFilterStatisticsTime(time, ZoneId.of("Europe/Moscow")))
        assertEquals("19.10.2026 20:38:14", formatFilterStatisticsTime(time, ZoneId.of("UTC")))
    }

    @Test fun removingEmptyMessageDoesNotDisableValidation() {
        val rule = newContentFilterRule(defaultBoardAddress())
        assertNull(filterConditionPresentation(ContentFilterMode.EXACT_PHRASE.name).error)
        assertEquals("Введите фразу целиком", filterConditionPresentation(ContentFilterMode.EXACT_PHRASE.name).hint)
        assertNotNull(contentFilterValidationError(rule))
        assertNull(contentFilterValidationError(newContentFilterRule(defaultBoardAddress(), "фраза")))
    }
}
