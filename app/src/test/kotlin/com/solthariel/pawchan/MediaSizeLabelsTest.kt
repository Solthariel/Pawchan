package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
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

/** Тред, о котором известен только заголовок (ещё не открывался): карточка без превью и счётчиков. */
private fun placeholderCatalogThread(
    address: ThreadAddress,
    subject: String?,
): CatalogThreadUi =
    CatalogThreadUi(
        address = address,
        content =
            CatalogThreadContentUi(
                subject = subject,
                text = CatalogThreadTextUi(""),
                author = null,
                date = null,
                sage = false,
                displaySubject = subject ?: "Тред №${address.thread.value}",
            ),
        attachments = emptyList(),
        flags = CatalogThreadFlagsUi(pinned = false, closed = false, endless = false),
        statistics = CatalogThreadStatisticsUi(0, 0, 0L, mediaBytesComplete = true),
    )
