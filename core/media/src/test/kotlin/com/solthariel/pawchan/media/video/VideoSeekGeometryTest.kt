package com.solthariel.pawchan.media.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSeekGeometryTest {
    @Test fun leftToRightProgressIsClampedToTheTrack() {
        assertEquals(0f, videoSeekTrackBoundary(200f, -0.5f, false), 0f)
        assertEquals(50f, videoSeekTrackBoundary(200f, 0.25f, false), 0f)
        assertEquals(200f, videoSeekTrackBoundary(200f, 2f, false), 0f)
    }

    @Test fun rightToLeftProgressIsClampedAndMirrored() {
        assertEquals(200f, videoSeekTrackBoundary(200f, -0.5f, true), 0f)
        assertEquals(150f, videoSeekTrackBoundary(200f, 0.25f, true), 0f)
        assertEquals(0f, videoSeekTrackBoundary(200f, 2f, true), 0f)
    }
}
