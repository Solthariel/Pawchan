package com.solthariel.pawchan

import android.widget.FrameLayout
import com.solthariel.pawchan.media.video.VideoAttempt
import com.solthariel.pawchan.media.video.VideoEngineBackend
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.reattach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VideoRecoveryTest {
    @Test
    fun playAfterTerminalFailureReopensTheSource() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                assertFalse(engine.opened.receive())
                engine.emit(VideoEvent.ERROR)
                assertTrue(engine.opened.receive())
                engine.emit(VideoEvent.ERROR)
                assertEquals(
                    com.solthariel.pawchan.media.video.VideoStage.ERROR,
                    session.status.value.stage,
                )
                session.togglePlay()
                assertTrue(engine.opened.receive())
                assertEquals(
                    com.solthariel.pawchan.media.video.VideoStage.BUFFERING,
                    session.status.value.stage,
                )
            } finally {
                session.close()
            }
        }

    @Test
    fun galleryReattachCanForcePausedPlayback() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                engine.opened.receive()
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true, seekable = true, duration = 5000L))
                session.reattach(false)
                assertEquals(1, engine.pauses)
                assertEquals(0L, engine.lastSeek)
            } finally {
                session.close()
            }
        }

    @Test
    fun fallbackIsBoundedWithProgressiveSource() {
        val initial = VideoAttempt()
        val software = initial.afterFailure()!!
        assertTrue(software.software)
        assertTrue(software.local)
        assertNull(software.afterFailure())
    }

    @Test
    fun hardwareFailureReopensSoftwareAndRestoresPositionWhenSeekable() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                assertFalse(engine.opened.receive())
                engine.emit(VideoEvent.PROGRESS, VideoPlaybackStatus(position = 4200L, duration = 10000L))
                engine.emit(VideoEvent.ERROR)
                assertTrue(engine.opened.receive())
                engine.emit(
                    VideoEvent.PLAYING,
                    VideoPlaybackStatus(duration = 10000L, seekable = true, playing = true),
                )
                assertEquals(4200L, engine.lastSeek)
                assertTrue(session.status.value.software)
            } finally {
                session.close()
            }
        }

    @Test
    fun backgroundPauseRemainsPausedWhenRetryStartsPlaying() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                engine.opened.receive()
                session.pause()
                session.setSoftware(true)
                assertTrue(engine.opened.receive())
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true))
                assertEquals(3, engine.pauses)
                session.close()
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true))
                assertEquals(3, engine.pauses)
            } finally {
                session.close()
            }
        }

    @Test
    fun thumbnailIsHeldUntilRenderedFrameAndNotReshownByBuffering() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                engine.opened.receive()
                engine.emit(
                    VideoEvent.PLAYING,
                    VideoPlaybackStatus(stage = com.solthariel.pawchan.media.video.VideoStage.READY, playing = true),
                )
                assertFalse(session.hasVideoOutput.value)
                engine.emit(VideoEvent.VIDEO_OUTPUT)
                assertTrue(session.hasVideoOutput.value)
                engine.emit(VideoEvent.PROGRESS)
                assertTrue(session.hasVideoOutput.value)
                engine.emit(VideoEvent.BUFFERING)
                assertTrue(session.hasVideoOutput.value)
                session.retry()
                assertFalse(session.hasVideoOutput.value)
            } finally {
                session.close()
            }
        }

    @Test
    fun pendingSurfaceCannotBecomeReadyFromPlaybackOrMetadataEvents() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                engine.opened.receive()
                engine.emit(VideoEvent.VIDEO_OUTPUT)
                assertTrue(session.hasVideoOutput.value)
                engine.emit(VideoEvent.VIDEO_OUTPUT_PENDING)
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true))
                engine.emit(VideoEvent.PROGRESS, VideoPlaybackStatus(seekable = true))
                engine.emit(VideoEvent.BUFFERING)
                assertFalse(session.hasVideoOutput.value)
                engine.emit(VideoEvent.VIDEO_OUTPUT)
                assertTrue(session.hasVideoOutput.value)
            } finally {
                session.close()
            }
        }

    @Test
    fun newSessionNeverInheritsAnotherSessionsRenderedOutput() =
        runTest {
            val oldEngine = TestVideoEngine()
            val nextEngine = TestVideoEngine()
            val old = VideoPlaybackSession(oldEngine, backgroundScope, VIDEO_TEST_URL)
            val next = VideoPlaybackSession(nextEngine, backgroundScope, VIDEO_TEST_URL)
            try {
                old.start()
                oldEngine.opened.receive()
                oldEngine.emit(VideoEvent.VIDEO_OUTPUT)
                next.start()
                nextEngine.opened.receive()
                assertTrue(old.hasVideoOutput.value)
                assertFalse(next.hasVideoOutput.value)
                nextEngine.emit(VideoEvent.VIDEO_OUTPUT)
                assertTrue(next.hasVideoOutput.value)
            } finally {
                old.close()
                next.close()
            }
        }

    @Test
    fun silentSessionIgnoresMuteAndDoesNotChangeRememberedPreference() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.start()
                engine.opened.receive()
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true))
                val before = session.sound.value.muted
                session.toggleMuted()
                assertEquals(before, session.sound.value.muted)
                engine.hasAudio = true
                engine.emit(VideoEvent.PROGRESS, VideoPlaybackStatus(playing = true))
                session.toggleMuted()
                assertEquals(!before, session.sound.value.muted)
                session.toggleMuted()
                assertEquals(before, session.sound.value.muted)
            } finally {
                session.close()
            }
        }

    @Test
    fun rejectedSpeedDoesNotChangeTheSpeedLabel() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                engine.acceptedSpeed = 1000
                session.setSpeed(com.solthariel.pawchan.media.video.VideoSpeed.FAST)
                assertEquals(com.solthariel.pawchan.media.video.VideoSpeed.NORMAL, session.sound.value.speed)
                engine.acceptedSpeed = null
                session.setSpeed(com.solthariel.pawchan.media.video.VideoSpeed.FAST)
                assertEquals(com.solthariel.pawchan.media.video.VideoSpeed.FAST, session.sound.value.speed)
            } finally {
                session.close()
            }
        }

    @Test
    fun playDuringLoadingSurvivesOpeningAndFirstReadyEvent() =
        runTest {
            val engine = TestVideoEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, VIDEO_TEST_URL)
            try {
                session.startAt(0, playing = false)
                engine.opened.receive()
                session.togglePlay()
                engine.emit(VideoEvent.PLAYING, VideoPlaybackStatus(playing = true, seekable = true))
                assertTrue(session.status.value.playing)
                assertEquals(1, engine.plays)
                assertEquals(1, engine.pauses)
            } finally {
                session.close()
            }
        }
}

private class TestVideoEngine : VideoEngineBackend {
    val opened = Channel<Boolean>(Channel.UNLIMITED)
    var lastSeek = -1L
    var pauses = 0
    var plays = 0
    var acceptedSpeed: Int? = null
    var hasAudio = false
    override val metadata: Map<String, String>
        get() = if (hasAudio) mapOf("audio_format" to "aac") else emptyMap()
    var listener: (VideoEvent, VideoPlaybackStatus) -> Unit = { _, _ -> }

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        listener = onEvent
        opened.trySend(software)
    }

    override fun pause() {
        pauses += 1
    }

    override fun seekTo(position: Long) {
        lastSeek = position
    }

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() {
        plays += 1
    }

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int) = acceptedSpeed ?: milli

    override fun stop() = Unit

    override fun close() = Unit
}

private const val VIDEO_TEST_URL = "https://example.test/video.mp4"

private fun TestVideoEngine.emit(
    type: VideoEvent,
    snapshot: VideoPlaybackStatus = VideoPlaybackStatus(),
) {
    listener(type, snapshot)
}
