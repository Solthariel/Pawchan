package com.solthariel.pawchan.media.video

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoContentGeometryTest {
    @Test fun landscapeAndPortraitFitWithoutCropping() {
        assertEquals(IntSize(1080, 608), videoContentSize(1080, 1920, 16.0 / 9.0))
        assertEquals(IntSize(608, 1080), videoContentSize(1920, 1080, 9.0 / 16.0))
        assertEquals(IntSize(100, 100), videoContentSize(100, 100, 1.0))
    }

    @Test fun rendererAspectWinsAndInvalidValuesUseMetadata() {
        assertEquals(0.5, videoContentAspect(0.5, 1920, 1080)!!, 0.0)
        assertEquals(16.0 / 9.0, videoContentAspect(Double.NaN, 1920, 1080)!!, 0.0)
        assertNull(videoContentAspect(Double.POSITIVE_INFINITY, 0, 1080))
        assertNull(videoContentAspect(null, null, null))
    }

    @Test fun unknownAspectAndTinySizesRemainSafe() {
        assertEquals(IntSize(1080, 1920), videoContentSize(1080, 1920, null))
        assertEquals(IntSize(0, 200), videoContentSize(0, 200, 1.0))
        assertEquals(IntSize(100, 1), videoContentSize(100, 100, 1e300))
        assertEquals(IntSize(1, 100), videoContentSize(100, 100, 1e-300))
    }
}
