package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDraftOptionsTest {
    @Test fun exactScaleFactorsIncludeOneEighthWithoutPercentRounding() {
        listOf(1, 2, 4, 8).forEach { divisor ->
            assertEquals(1f / divisor, DraftImageReencoding(scaleDivisor = divisor).scaleFactor, 0f)
        }
    }

    @Test fun oldPercentageSettingsStillWork() {
        assertEquals(0.5f, DraftImageReencoding(scalePercent = 50).scaleFactor, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDivisorIsRejected() {
        DraftImageReencoding(scaleDivisor = 3)
    }
}
