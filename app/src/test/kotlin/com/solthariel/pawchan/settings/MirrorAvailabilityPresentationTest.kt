package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class MirrorAvailabilityPresentationTest {
    @Test fun emptyResultsDoNotInventATimeOrStatus() {
        assertEquals(emptyList<MirrorStatusRow>(), mirrorAvailabilityPresentation(emptyList()).rows)
        assertNull(mirrorAvailabilityPresentation(emptyList()).checkedCaption)
    }

    @Test fun resultsPreserveHostsStatusesAndUseOneLatestLocalTime() {
        val result =
            mirrorAvailabilityPresentation(
                listOf(
                    MirrorObservation("2ch.su", MirrorAvailability.AVAILABLE, 1_000),
                    MirrorObservation("2ch.org", MirrorAvailability.TIMEOUT, 5_000),
                ),
                ZoneOffset.ofHours(3),
            )
        assertEquals(
            listOf(MirrorStatusRow("2ch.su", "Доступно"), MirrorStatusRow("2ch.org", "Нет ответа вовремя")),
            result.rows,
        )
        assertEquals("Проверено с этого устройства в 03:00:05", result.checkedCaption)
    }

    @Test fun responseTimeIsOptionalAndSummaryCountsOnlyWorkingMirrors() {
        val result =
            mirrorAvailabilityPresentation(
                listOf(
                    MirrorObservation("2ch.su", MirrorAvailability.AVAILABLE, 1_000, 123),
                    MirrorObservation("2ch.org", MirrorAvailability.TIMEOUT, 1_000),
                    MirrorObservation("2ch.life", MirrorAvailability.SERVER_ERROR, 1_000, 0),
                ),
                ZoneOffset.UTC,
            )
        assertEquals("HTTP-проверка: 123 мс", result.rows[0].responseCaption)
        assertNull(result.rows[1].responseCaption)
        assertEquals("HTTP-проверка: 0 мс", result.rows[2].responseCaption)
        assertEquals("Доступны 1 из 3 — проверено в 00:00:01", result.summary)
    }

    @Test fun activeHostIsNotDuplicatedUnlessRecoveryUsesAnotherMirror() {
        assertNull(activeMirrorCaption("2ch.su", "2ch.su"))
        assertEquals("Сейчас используется: 2ch.life", activeMirrorCaption("2ch.su", "2ch.life"))
    }
}
