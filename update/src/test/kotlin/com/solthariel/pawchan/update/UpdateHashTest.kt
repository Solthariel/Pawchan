package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateHashTest {
    @Test
    fun normalizesColonSeparatedUppercaseHash() {
        assertEquals("aabbcc", normalizeHash("AA:BB:CC"))
    }

    @Test
    fun calculatesSha256WithoutPlatformDependencies() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256("abc".encodeToByteArray()),
        )
    }
}
