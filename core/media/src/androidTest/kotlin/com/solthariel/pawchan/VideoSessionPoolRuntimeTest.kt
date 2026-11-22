package com.solthariel.pawchan

import android.widget.FrameLayout
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.media.video.VideoEngineBackend
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoSessionPool
import com.solthariel.pawchan.media.video.ownedVideoSession
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoSessionPoolRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun preloadingOneVideoEvictsTheOldSpare() {
        val old = PoolEngine()
        compose.setContent { Unit }
        compose.runOnIdle {
            VideoSessionPool.clear()
            try {
                VideoSessionPool.retain(ownedVideoSession(old, "https://example.test/old"))
                VideoSessionPool.preload("https://example.test/next") {
                    ownedVideoSession(PoolEngine(), "https://example.test/next")
                }
                assertTrue(old.closed)
                assertNull(VideoSessionPool.take("https://example.test/old"))
                assertNotNull(VideoSessionPool.take("https://example.test/next")?.first?.also { it.close() })
            } finally {
                VideoSessionPool.clear()
            }
        }
    }

    @Test fun retainingPreviousVideoDoesNotEvictPreparedNextVideo() {
        val previous = PoolEngine()
        compose.setContent { Unit }
        compose.runOnIdle {
            VideoSessionPool.clear()
            try {
                VideoSessionPool.preload("https://example.test/next") {
                    ownedVideoSession(PoolEngine(), "https://example.test/next")
                }
                VideoSessionPool.retain(ownedVideoSession(previous, "https://example.test/old"))
                assertTrue(previous.closed)
                assertNull(VideoSessionPool.take("https://example.test/old"))
                assertNotNull(VideoSessionPool.take("https://example.test/next")?.first?.also { it.close() })
            } finally {
                VideoSessionPool.clear()
            }
        }
    }
}

private class PoolEngine : VideoEngineBackend {
    var closed = false
        private set

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) = Unit

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(position: Long) = Unit

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int): Int = milli

    override fun stop() = Unit

    override fun close() {
        closed = true
    }
}
