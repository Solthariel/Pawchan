package com.solthariel.pawchan

import com.solthariel.pawchan.media.thumbnail.thumbnailSampleSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailSampleSizeTest {
    @Test
    fun smallThumbnailIsNotDownsampled() {
        assertEquals(1, thumbnailSampleSize(200, 120, 384))
    }

    @Test
    fun largeImagesAndPanoramasHaveBoundedDecodedSide() {
        for (size in listOf(1_024, 4_096, 32_768, 100_000)) {
            val sample = thumbnailSampleSize(size, 100, 384)
            assertTrue(size / sample <= 384)
            assertEquals(0, sample and (sample - 1))
        }
    }
}
