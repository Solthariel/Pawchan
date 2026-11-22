package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.pollPlaybackStage
import com.solthariel.pawchan.media.video.reportedHardwareDecoder
import com.solthariel.pawchan.media.video.shouldRefreshMpvDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvPollSampleTest {
    @Test
    fun bufferingAndEndedStagesMatchPlaybackState() {
        assertEquals(VideoStage.BUFFERING, pollPlaybackStage(false, false, false))
        assertEquals(VideoStage.BUFFERING, pollPlaybackStage(false, true, true))
        assertEquals(VideoStage.READY, pollPlaybackStage(false, true, false))
        assertEquals(VideoStage.READY, pollPlaybackStage(true, false, true))
    }

    @Test
    fun detailedMetadataIsRefreshedOnOpenAndPeriodically() {
        assertTrue(shouldRefreshMpvDetails(false, 0L))
        assertTrue(shouldRefreshMpvDetails(false, 1_000L))
        assertFalse(shouldRefreshMpvDetails(true, 0L))
        assertFalse(shouldRefreshMpvDetails(true, 1_999L))
        assertTrue(shouldRefreshMpvDetails(true, 2_000L))
    }

    @Test
    fun reportsDecoderOnlyAfterFileIsOpen() {
        assertNull(reportedHardwareDecoder(false, "mediacodec"))
        assertNull(reportedHardwareDecoder(true, null))
        assertEquals(false, reportedHardwareDecoder(true, "no"))
        assertEquals(true, reportedHardwareDecoder(true, "mediacodec"))
    }
}
