package com.solthariel.pawchan.library

import com.solthariel.pawchan.domain.boards.ArchivedThreadSummary
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ArchiveThreadPresentationTest {
    private val address =
        ThreadAddress(
            BoardAddress(SiteId.parse("dvach")!!, BoardId.parse("mobi")!!),
            ThreadId.parse(42)!!,
        )

    @Test fun datesUseExplicitTimezoneAndDoNotInventCounts() {
        val row = presentArchiveThread(ArchivedThreadSummary(address, "Тема", 1L, "2020-05-09"), ZoneId.of("UTC"))
        assertEquals("Тема", row.subject)
        assertTrue(row.metadata.contains("№42"))
        assertTrue(row.metadata.contains("01.01.70 00:00"))
        assertTrue(row.metadata.contains("2020-05-09"))
        assertFalse(row.metadata.contains("ответов"))
    }

    @Test fun missingDatesStayMissing() {
        assertEquals("№42", presentArchiveThread(ArchivedThreadSummary(address, "Без темы")).metadata)
    }
}
