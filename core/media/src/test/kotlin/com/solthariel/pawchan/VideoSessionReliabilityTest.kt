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
import org.junit.Test

class VideoSessionReliabilityTest {
    @Test
    fun onlyLatestSeekWhileLoadingIsAppliedWhenReady() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.start()
                testScheduler.runCurrent()
                session.seekTo(2000)
                session.seekTo(7000)
                assertEquals(emptyList<Long>(), engine.seeks)
                engine.listeners.last()(VideoEvent.PLAYING, READY)
                engine.listeners.last()(VideoEvent.PROGRESS, READY)
                assertEquals(listOf(7000L), engine.seeks)
            } finally {
                session.close()
            }
        }

    @Test
    fun switchingDecoderKeepsPendingStartPositionAndClampsToDuration() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.startAt(15000)
                testScheduler.runCurrent()
                session.setSoftware(true)
                testScheduler.runCurrent()
                engine.listeners.last()(VideoEvent.PLAYING, READY)
                assertEquals(listOf(10000L), engine.seeks)
            } finally {
                session.close()
            }
        }

    @Test
    fun retryKeepsTheLatestSeekRequestedWhileLoading() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.start()
                testScheduler.runCurrent()
                session.seekTo(6000)
                session.retry()
                testScheduler.runCurrent()
                engine.listeners.last()(VideoEvent.PLAYING, READY)
                assertEquals(listOf(6000L), engine.seeks)
            } finally {
                session.close()
            }
        }

    @Test
    fun failedHardwareCallbacksCannotTerminateQueuedSoftwareRecovery() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.start()
                testScheduler.runCurrent()
                val hardware = engine.listeners.single()
                hardware(VideoEvent.ERROR, READY)
                hardware(VideoEvent.ERROR, READY)
                hardware(VideoEvent.VIDEO_OUTPUT, READY)
                assertEquals(VideoStage.BUFFERING, session.status.value.stage)
                assertFalse(session.hasVideoOutput.value)
                testScheduler.runCurrent()
                assertEquals(listOf(false, true), engine.software)
                engine.listeners.last()(VideoEvent.PLAYING, READY)
                hardware(VideoEvent.PROGRESS, READY.copy(position = 9999))
                assertEquals(0L, session.status.value.position)
            } finally {
                session.close()
            }
        }

    @Test
    fun closeCancelsQueuedOpenAndReleasesBackendOnlyOnce() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            session.start()
            session.close()
            session.close()
            testScheduler.runCurrent()
            assertEquals(0, engine.listeners.size)
            assertEquals(1, engine.closes)
        }

    @Test
    fun rapidPlayPauseDuringLoadingUsesIntentInsteadOfDelayedSnapshot() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.startAt(0, playing = false)
                testScheduler.runCurrent()
                session.togglePlay()
                session.togglePlay()
                assertEquals(1, engine.plays)
                assertEquals(2, engine.pauses)
            } finally {
                session.close()
            }
        }

    @Test
    fun synchronousOpenFailureCanBeRetriedWithPlay() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                engine.failOpen = true
                session.start()
                testScheduler.runCurrent()
                assertEquals(VideoStage.ERROR, session.status.value.stage)
                engine.failOpen = false
                session.togglePlay()
                testScheduler.runCurrent()
                assertEquals(2, engine.listeners.size)
            } finally {
                session.close()
            }
        }

    @Test
    fun rapidRestartsCancelOlderQueuedOpens() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                repeat(3) { session.start() }
                testScheduler.runCurrent()
                assertEquals(1, engine.listeners.size)
            } finally {
                session.close()
            }
        }

    @Test
    fun closedSessionIgnoresLateEventsAndPlaybackCommands() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            session.start()
            testScheduler.runCurrent()
            session.close()
            val before = session.status.value
            session.seekTo(3000)
            session.retry()
            session.setSoftware(true)
            session.togglePlay()
            session.pause()
            engine.listeners.single()(VideoEvent.VIDEO_OUTPUT, READY)
            testScheduler.runCurrent()
            assertEquals(before, session.status.value)
            assertFalse(session.hasVideoOutput.value)
            assertEquals(1, engine.listeners.size)
            assertEquals(0, engine.plays)
            assertEquals(0, engine.pauses)
            assertEquals(emptyList<Long>(), engine.seeks)
        }

    @Test
    fun completionStillAllowsReplayAfterIntentBasedToggle() =
        runTest {
            val engine = ReliabilityEngine()
            val session = VideoPlaybackSession(engine, backgroundScope, "file:///movie.mp4")
            try {
                session.start()
                testScheduler.runCurrent()
                engine.listeners.last()(VideoEvent.ENDED, READY.copy(ended = true, playing = false))
                session.togglePlay()
                testScheduler.runCurrent()
                assertEquals(2, engine.listeners.size)
                assertEquals(listOf(0L), engine.seeks)
            } finally {
                session.close()
            }
        }
}

private class ReliabilityEngine : VideoEngineBackend {
    val listeners = mutableListOf<(VideoEvent, VideoPlaybackStatus) -> Unit>()
    val software = mutableListOf<Boolean>()
    val seeks = mutableListOf<Long>()
    var failOpen = false
    var closes = 0
    var pauses = 0
    var plays = 0

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        listeners += onEvent
        this.software += software
        if (failOpen) throw java.io.IOException("test opening failure")
    }

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() {
        plays++
    }

    override fun pause() {
        pauses++
    }

    override fun seekTo(position: Long) {
        seeks += position
    }

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int) = milli

    override fun stop() = Unit

    override fun close() {
        closes++
    }
}

private val READY = VideoPlaybackStatus(stage = VideoStage.READY, duration = 10000, seekable = true, playing = true)
