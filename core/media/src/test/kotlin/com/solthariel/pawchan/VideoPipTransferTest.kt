package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.commitPipTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPipTransferTest {
    @Test fun rejectedRequestKeepsTheOriginalSurface() {
        var transfers = 0
        assertFalse(commitPipTransfer(false) { transfers++ })
        assertEquals(0, transfers)
    }

    @Test fun acceptedRequestTransfersExactlyOnce() {
        var transfers = 0
        assertTrue(commitPipTransfer(true) { transfers++ })
        assertEquals(1, transfers)
    }
}
