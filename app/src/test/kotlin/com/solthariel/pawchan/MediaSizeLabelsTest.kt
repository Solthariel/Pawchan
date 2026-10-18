package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MediaSizeLabelsTest {
    @Test
    fun catalogHidesInequalityButPreservesIncompleteMetadata() {
        val statistics = CatalogThreadStatisticsUi(12, 3, 1024L, false)
        assertEquals(formatByteSizeForUi(1024L), statistics.sizeText)
        assertFalse(statistics.mediaBytesComplete)
        assertEquals("", CatalogThreadStatisticsUi(12, 3, 0L, false).sizeText)
    }
}
