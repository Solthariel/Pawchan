package com.solthariel.pawchan.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPickerPlaybackTest {
    @Test fun playingVideoResumesOnceWhenResultArrivesBeforeForeground() {
        val picker = MediaPickerPlayback()
        var resumes = 0
        picker.bind({ true }, { resumes++ }, true)
        picker.started()
        picker.stopped()
        picker.completed()
        assertEquals(0, resumes)
        picker.returned()
        picker.returned()
        assertEquals(1, resumes)
    }

    @Test fun foregroundBeforeResultAlsoRestoresPlaybackOnce() {
        val picker = MediaPickerPlayback()
        var resumes = 0
        picker.bind({ true }, { resumes++ }, true)
        picker.started()
        picker.stopped()
        picker.returned()
        assertEquals(0, resumes)
        picker.completed()
        picker.completed()
        assertEquals(1, resumes)
    }

    @Test fun userPausedVideoAndOrdinaryBackgroundingNeverAutoPlay() {
        val picker = MediaPickerPlayback()
        var resumes = 0
        picker.bind({ false }, { resumes++ }, true)
        picker.started()
        picker.stopped()
        picker.completed()
        picker.returned()
        picker.stopped()
        picker.returned()
        assertEquals(0, resumes)
    }

    @Test fun switchingOrClosingTheViewerDiscardsThePreviousIntent() {
        val picker = MediaPickerPlayback()
        var resumes = 0
        picker.bind({ true }, { resumes++ }, true)
        picker.started()
        picker.stopped()
        picker.clear()
        picker.bind({ true }, { resumes++ }, false)
        picker.completed()
        picker.returned()
        assertEquals(0, resumes)
    }

    @Test fun duplicateLaunchDoesNotReplaceTheOriginalPlayingIntent() {
        val picker = MediaPickerPlayback()
        var playing = true
        var resumes = 0
        picker.bind({ playing }, { resumes++ }, true)
        picker.started()
        playing = false
        picker.started()
        picker.stopped()
        picker.completed()
        picker.returned()
        assertEquals(1, resumes)
    }
}
