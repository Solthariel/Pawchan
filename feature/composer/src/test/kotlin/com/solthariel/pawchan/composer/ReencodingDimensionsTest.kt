package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReencodingDimensionsTest {
    @Test fun reductionMatchesEncoderRoundingAndMinimumPixel() {
        assertEquals(
            ReencodingDimensions(270, 480),
            reencodingDimensions(1080, 1920, ImageReencoding(scaleDivisor = 4)),
        )
        assertEquals(ReencodingDimensions(3, 1), reencodingDimensions(5, 1, ImageReencoding(scaleDivisor = 2)))
        assertNull(reencodingDimensions(0, 50, ImageReencoding()))
        assertNull(reencodingDimensions(50, -1, ImageReencoding()))
    }

    @Test fun legacyPercentageAndExifAreRespected() {
        val dimensions = requireNotNull(reencodingDimensions(1000, 500, ImageReencoding(scalePercent = 90)))
        assertEquals(ReencodingDimensions(900, 450), dimensions)
        for (orientation in 1..8) {
            assertEquals(
                if (orientation in 5..8) ReencodingDimensions(450, 900) else dimensions,
                dimensions.oriented(orientation),
            )
        }
    }

    @Test fun pendingNameHonorsFormatCustomNameAndAnonymizationWithoutChangingOriginal() {
        val original = ComposerAttachment(ReplyAttachment("photo.webp", "image/webp", byteArrayOf(1)))
        val pending = original.copy(options = original.options.copy(reencoding = ImageReencoding(jpeg = false)))
        assertEquals("photo.png", pending.displayName)
        assertEquals("renamed.png", pending.copy(options = pending.options.copy(customName = "renamed")).displayName)
        assertEquals("file.png", pending.copy(options = pending.options.copy(removeFileName = true)).displayName)
        assertEquals("photo.webp", original.displayName)
        assertNull(original.options.reencoding)
    }
}
