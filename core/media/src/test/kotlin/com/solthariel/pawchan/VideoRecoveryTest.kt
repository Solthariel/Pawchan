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
}

private class TestVideoEngine : VideoEngineBackend {
    val opened = Channel<Boolean>(Channel.UNLIMITED)
    var lastSeek = -1L
    var pauses = 0
    private var listener: (VideoEvent, VideoPlaybackStatus) -> Unit = { _, _ -> }

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        listener = onEvent
        opened.trySend(software)
    }

    fun emit(
        type: VideoEvent,
        snapshot: VideoPlaybackStatus = VideoPlaybackStatus(),
    ) {
        listener(type, snapshot)
    }

    override fun pause() {
        pauses += 1
    }

    override fun seekTo(position: Long) {
        lastSeek = position
    }

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() = Unit

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int) = Unit

    override fun stop() = Unit

    override fun close() = Unit
}

private const val VIDEO_TEST_URL = "https://example.test/video.mp4"
