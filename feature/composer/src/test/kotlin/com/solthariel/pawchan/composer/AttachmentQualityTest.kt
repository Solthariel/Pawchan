package com.solthariel.pawchan.composer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AttachmentQualityTest {
    @Test
    fun jpegQualityDefaultsToMaximumAndAcceptsTheSliderRange() {
        assertEquals(100, ImageReencoding().quality)
        assertEquals(1, ImageReencoding(quality = 1).quality)
        assertEquals(87, ImageReencoding(quality = 87).quality)
        assertThrows(IllegalArgumentException::class.java) { ImageReencoding(quality = 0) }
        assertThrows(IllegalArgumentException::class.java) { ImageReencoding(quality = 101) }
    }
}
