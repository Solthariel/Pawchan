package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.navigation.defaultThreadAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WorkspaceReadingMemoryTest {
    @Test fun sessionCacheIsBoundedAndReadingOffsetsAreNotResetByRefresh() {
        val reading = WorkspaceReadingMemory()
        val first = defaultThreadAddress(100)
        reading.position(first, 105, 33)
        (100L..106L).forEach {
            reading.remember(
                ThreadSnapshot(defaultThreadAddress(it), "title", ThreadFlags(false, false, false), emptyList()),
            )
        }
        assertNull(reading.snapshot(first))
        assertNotNull(reading.snapshot(defaultThreadAddress(106)))
        assertEquals(105L to 33, reading.position(first))
    }

    @Test fun offsetsAreNonNegative() {
        val reading = WorkspaceReadingMemory()
        val address = defaultThreadAddress(100)
        reading.position(address, 105, -30)
        assertEquals(105L to 0, reading.position(address))
    }
}
