package com.solthariel.pawchan.threads

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleSectionRowsTest {
    private val records = (1..100).toList()

    @Test fun collapsingOnlyLimitsThePresentationAndDoesNotMutateRecords() {
        assertEquals((1..5).toList(), visibleSectionRows(records, 5, false))
        assertEquals(100, records.size)
    }

    @Test fun expandingShowsAllRemainingRecordsNotTheNextBatch() {
        assertEquals(records, visibleSectionRows(records, 5, true))
        assertEquals((1..5).toList(), visibleSectionRows(records, 5, false))
    }

    @Test fun searchUsesAllRowsAndSmallSectionsHaveNoArtificialEntries() {
        assertEquals(records, visibleSectionRows(records, 1, true))
        assertEquals(listOf(1, 2), visibleSectionRows(listOf(1, 2), 20, false))
        assertEquals(emptyList<Int>(), visibleSectionRows(emptyList<Int>(), 5, false))
    }
}
