package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.vocaroo.TAG
import com.solthariel.pawchan.infrastructure.vocaroo.elapsedMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocarooLogTimingTest {
    @Test fun loggingTagAndElapsedTimeRemainAvailableWithoutCustomDns() {
        assertEquals("Vocaroo", TAG)
        assertTrue(elapsedMillis(System.nanoTime() - 7_000_000L) >= 7L)
    }
}
