package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.videoDurationLabel
import com.solthariel.pawchan.media.video.videoTimeLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDurationLabelTest {
    @Test
    fun unknownDurationIsNotShownAsARealTime() {
        assertEquals("—:—", videoDurationLabel(0L))
        assertEquals("—:—", videoDurationLabel(-1L))
    }

    @Test
    fun confirmedDurationKeepsExistingWholeSecondFormatting() {
        assertEquals("0:12", videoDurationLabel(12957L))
        assertEquals("1:05", videoDurationLabel(65000L))
    }

    @Test
    fun currentPositionStillStartsAtZeroAndSupportsLongVideos() {
        assertEquals("0:00", videoTimeLabel(0L))
        assertEquals("0:00", videoTimeLabel(-1L))
        assertEquals("62:03", videoTimeLabel(3723000L))
    }
}
