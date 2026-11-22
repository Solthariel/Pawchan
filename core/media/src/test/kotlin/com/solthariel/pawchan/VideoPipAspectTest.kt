package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.mpvDisplayAspect
import com.solthariel.pawchan.media.video.videoPipAspect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoPipAspectTest {
    @Test fun decoderAspectReflectsRotation() {
        assertEquals(9.0 / 16.0, mpvDisplayAspect(16.0 / 9.0, 90)!!, 0.00001)
        assertEquals(9.0 / 16.0, mpvDisplayAspect(16.0 / 9.0, -90)!!, 0.00001)
        assertEquals(16.0 / 9.0, mpvDisplayAspect(16.0 / 9.0, 180)!!, 0.00001)
        assertNull(mpvDisplayAspect(Double.NaN, 0))
        assertNull(mpvDisplayAspect(0.0, 90))
    }

    @Test fun decoderWinsOverStaleDimensions() {
        assertEquals(9.0 / 16.0, videoPipAspect(9.0 / 16.0, 1920, 1080), 0.00001)
        assertEquals(1.5, videoPipAspect(null, 300, 200), 0.00001)
    }

    @Test fun invalidAndExtremeValuesAreSafeForAndroid() {
        assertEquals(16.0 / 9.0, videoPipAspect(Double.NaN, 0, 0), 0.00001)
        assertEquals(16.0 / 9.0, videoPipAspect(null, -2, 100), 0.00001)
        assertEquals(2.35, videoPipAspect(Double.POSITIVE_INFINITY, 5000, 1), 0.00001)
        assertEquals(1.0 / 2.35, videoPipAspect(0.001, null, null), 0.00001)
    }
}
