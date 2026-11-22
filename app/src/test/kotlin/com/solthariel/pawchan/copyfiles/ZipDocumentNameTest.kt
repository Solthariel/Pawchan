package com.solthariel.pawchan.copyfiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipDocumentNameTest {
    @Test fun shortNamesRemainIntact() {
        assertEquals("thread.zip", zipDisplayName("thread.zip"))
        assertEquals("a".repeat(30) + ".zip", zipDisplayName("a".repeat(30) + ".zip"))
    }

    @Test fun longNamesKeepBeginningAndExtension() {
        val result = zipDisplayName("thread-b-328868282-1795233933901.zip")
        assertTrue(result.startsWith("thread-b-328868282-"))
        assertTrue(result.contains("…"))
        assertTrue(result.endsWith(".zip"))
    }

    @Test fun emojiSurrogatePairsAreNotSplit() {
        val result = zipDisplayName("😀".repeat(40) + ".zip")
        assertFalse(result.any { it == '\uFFFD' })
        assertEquals(result, String(result.toByteArray(Charsets.UTF_8), Charsets.UTF_8))
        assertTrue(result.endsWith(".zip"))
    }
}
