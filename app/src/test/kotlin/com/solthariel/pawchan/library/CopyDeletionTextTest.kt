package com.solthariel.pawchan.library

import org.junit.Assert.assertEquals
import org.junit.Test

class CopyDeletionTextTest {
    @Test fun russianFormsCoverSingularPluralAndTeens() {
        assertEquals("Будут удалены 0 копий.", copyDeletionLead(0))
        assertEquals("Будет удалена 1 копия.", copyDeletionLead(1))
        assertEquals("Будут удалены 2 копии.", copyDeletionLead(2))
        assertEquals("Будут удалены 5 копий.", copyDeletionLead(5))
        assertEquals("Будут удалены 11 копий.", copyDeletionLead(11))
        assertEquals("Будут удалены 14 копий.", copyDeletionLead(14))
        assertEquals("Будет удалена 21 копия.", copyDeletionLead(21))
        assertEquals("Будут удалены 112 копий.", copyDeletionLead(112))
    }
}
