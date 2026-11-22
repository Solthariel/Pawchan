package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.placeholderCatalogThread
import com.solthariel.pawchan.catalog.toThreadListCardUi
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MediaSizeLabelsTest {
    @Test
    fun boardListOmitsSizesButPreservesCountsAndTheSavedCopyLabel() {
        val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
        val base = placeholderCatalogThread(address, null)
        for (bytes in listOf(575_000L, 20_000_000L)) {
            val thread = base.copy(statistics = CatalogThreadStatisticsUi(1000, 125, bytes, false))
            val counts = thread.toThreadListCardUi().counts
            assertEquals("", counts.sizeText)
            assertEquals("1000", counts.repliesText)
            assertEquals("125", counts.filesText)
            val saved = thread.toThreadListCardUi(sizeText = thread.statistics.sizeText)
            assertEquals(formatByteSizeForUi(bytes), saved.counts.sizeText)
        }
    }

    @Test
    fun catalogHidesInequalityButPreservesIncompleteMetadata() {
        val statistics = CatalogThreadStatisticsUi(12, 3, 1024L, false)
        assertEquals(formatByteSizeForUi(1024L), statistics.sizeText)
        assertFalse(statistics.mediaBytesComplete)
        assertEquals("", CatalogThreadStatisticsUi(12, 3, 0L, false).sizeText)
    }
}
