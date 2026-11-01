package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColorSeedInputTest {
    @Test fun acceptsSixRgbDigitsWithOptionalHashAndWhitespace() {
        assertEquals(RGB_EXAMPLE, parseSeedHex(" #23a080 "))
        assertEquals(RGB_EXAMPLE, parseSeedHex("23A080"))
        assertEquals(0, parseSeedHex("000000"))
        assertEquals(RGB_WHITE, parseSeedHex("FFFFFF"))
    }

    @Test fun rejectsIncompleteNonHexAndAlphaInput() {
        listOf("", "#", "#fff", "GG0000", "FF23A080", "-12345", "１２３４５６").forEach { assertNull(parseSeedHex(it)) }
    }

    @Test fun formattingRoundTripsAndKeepsLeadingZeroes() {
        assertEquals("#000000", seedHex(0))
        assertEquals("#00000F", seedHex(HEX_DIGIT_F))
        listOf(0, HEX_DIGIT_F, RGB_EXAMPLE, RGB_WHITE).forEach { assertEquals(it, parseSeedHex(seedHex(it))) }
    }
}

private const val RGB_EXAMPLE = 0x23A080
private const val RGB_WHITE = 0xFFFFFF
private const val HEX_DIGIT_F = 15
