package com.solthariel.pawchan

import android.widget.FrameLayout
import com.solthariel.pawchan.media.video.VideoEngineBackend
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoIncompleteTest {
    @Test
    fun incompleteInputPausesWithoutCompletionOrDecoderFallback() =
        runTest {
            val engine = IncompleteTestEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "https://example.test/video.webm")
            try {
                session.start()
                testScheduler.runCurrent()
                assertEquals(1, engine.opens)
                engine.listener(
                    VideoEvent.INCOMPLETE,
                    VideoPlaybackStatus(stage = VideoStage.READY, position = 12920, duration = 12957, seekable = true),
                )
                assertEquals(1, engine.opens)
                assertEquals(VideoStage.READY, session.status.value.stage)
                assertFalse(session.status.value.ended)
                assertFalse(session.status.value.playing)
                assertFalse(session.status.value.software)
                assertEquals(12957L, session.status.value.duration)
                session.seekTo(5000)
                assertEquals(5000L, engine.seek)
                session.togglePlay()
                assertEquals(1, engine.plays)
            } finally {
                session.close()
            }
        }

    @Test
    fun unknownDurationDoesNotBlockPlaybackButBlocksSeekUntilConfirmed() =
        runTest {
            val engine = IncompleteTestEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "https://example.test/video.webm")
            try {
                session.start()
                testScheduler.runCurrent()
                engine.listener(
                    VideoEvent.PLAYING,
                    VideoPlaybackStatus(stage = VideoStage.READY, position = 200, playing = true),
                )
                assertTrue(session.status.value.playing)
                assertFalse(session.status.value.seekable)
                session.seekTo(5000)
                assertEquals(-1L, engine.seek)
                engine.listener(
                    VideoEvent.PROGRESS,
                    VideoPlaybackStatus(stage = VideoStage.READY, duration = 12957, playing = true, seekable = true),
                )
                session.seekTo(5000)
                assertEquals(5000L, engine.seek)
            } finally {
                session.close()
            }
        }

    @Test
    fun normalCompletionStillHasItsNormalMeaning() =
        runTest {
            val engine = IncompleteTestEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "https://example.test/video.mp4")
            try {
                session.start()
                testScheduler.runCurrent()
                engine.listener(VideoEvent.ENDED, VideoPlaybackStatus(position = 1200, duration = 1200, ended = true))
                assertTrue(session.status.value.ended)
                assertEquals(1, engine.opens)
            } finally {
                session.close()
            }
        }
}

private class IncompleteTestEngine : VideoEngineBackend {
    var opens = 0
    var plays = 0
    var seek = -1L
    var listener: (VideoEvent, VideoPlaybackStatus) -> Unit = { _, _ -> }

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        opens++
        listener = onEvent
    }

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() {
        plays++
    }

    override fun pause() = Unit

    override fun seekTo(position: Long) {
        seek = position
    }

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int): Int = milli

    override fun stop() = Unit

    override fun close() = Unit
}
