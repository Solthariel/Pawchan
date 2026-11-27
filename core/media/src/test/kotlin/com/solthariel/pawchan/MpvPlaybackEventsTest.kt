package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.MpvPlaybackEvents
import com.solthariel.pawchan.media.video.MpvPollSample
import com.solthariel.pawchan.media.video.MpvPollTiming
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoFailure
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvPlaybackEventsTest {
    private val events = MpvPlaybackEvents()
    private val received = mutableListOf<Pair<VideoEvent, VideoPlaybackStatus>>()
    private val playing = MpvPollSample(MpvPollTiming(700, 3000), true, false, false, 1.5, "mediacodec", emptyMap())

    @Test
    fun backgroundReadAheadIsNotShownAsBufferingWhilePlaybackAdvancesOrIsPaused() {
        events.open { event, status -> received.add(event to status) }
        events.sample(playing, waitingForBytes = true)
        assertEquals(VideoStage.READY, received.last().second.stage)
        events.sample(playing, waitingForBytes = true)
        assertEquals(VideoStage.BUFFERING, received.last().second.stage)
        events.sample(playing.copy(timing = MpvPollTiming(900, 3000)), waitingForBytes = true)
        assertEquals(VideoStage.READY, received.last().second.stage)
        events.sample(playing.copy(paused = true), waitingForBytes = true)
        events.sample(playing.copy(paused = true), waitingForBytes = true)
        assertEquals(VideoStage.READY, received.last().second.stage)
        events.sourceError()
        assertEquals(VideoEvent.ERROR, received.last().first)
        assertEquals(
            VideoFailure.SOURCE,
            received
                .last()
                .second.attempt.failure,
        )
    }

    @Test
    fun pollUpdatesTimelineAndDecoderStatus() {
        events.open { event, status -> received.add(event to status) }
        events.sample(playing)
        val (event, status) = received.single()
        assertEquals(VideoEvent.PROGRESS, event)
        assertEquals(VideoStage.READY, status.stage)
        assertEquals(700L, status.position)
        assertEquals(3000L, status.duration)
        assertTrue(status.playing)
        assertTrue(status.seekable)
        assertEquals(true, status.hardwareDecoded)
        assertEquals(1.5, checkNotNull(status.videoAspect), 0.0)
    }

    @Test
    fun eofRemainsEndedUntilPlayOrSeekAndErrorsAreDelivered() {
        events.open { event, status -> received.add(event to status) }
        events.sample(playing)
        events.end("eof")
        assertEquals(VideoEvent.ENDED, received.last().first)
        assertTrue(received.last().second.ended)
        assertFalse(received.last().second.playing)
        events.sample(playing)
        assertTrue(received.last().second.ended)
        assertFalse(received.last().second.playing)
        events.resumeOrSeek()
        events.sample(playing)
        assertFalse(received.last().second.ended)
        assertTrue(received.last().second.playing)
        events.end("error")
        assertEquals(VideoEvent.ERROR, received.last().first)
        val count = received.size
        events.end("stop")
        assertEquals(count, received.size)
    }

    @Test
    fun reopeningResetsSnapshotAndSwitchesCallbackAndCloseStopsDelivery() {
        events.open { event, status -> received.add(event to status) }
        events.sample(playing)
        events.end("eof")
        val previousCount = received.size
        val next = mutableListOf<Pair<VideoEvent, VideoPlaybackStatus>>()
        events.open { event, status -> next.add(event to status) }
        events.emit(VideoEvent.OPENING)
        assertEquals(previousCount, received.size)
        assertEquals(VideoEvent.OPENING, next.single().first)
        assertEquals(VideoPlaybackStatus(), next.single().second)
        events.close()
        events.emit(VideoEvent.ERROR)
        assertEquals(1, next.size)
    }
}
