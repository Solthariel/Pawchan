package com.solthariel.pawchan.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoredThreadAddressTest {
    @Test
    fun validStoredFieldsPreserveTheFullAddress() {
        val address = requireNotNull(storedThreadAddress("2ch", "test", 123))
        assertEquals("2ch", address.site.value)
        assertEquals("test", address.board.board.value)
        assertEquals(123L, address.thread.value)
    }

    @Test
    fun invalidStoredFieldsDoNotCreateAnAddress() {
        assertNull(storedThreadAddress("", "b", 1))
        assertNull(storedThreadAddress("2ch", "bad/board", 1))
        assertNull(storedThreadAddress("2ch", "b", 0))
    }
}
