package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.video.LocalVideoPosterStore
import com.solthariel.pawchan.media.video.VideoPosterStore
import com.solthariel.pawchan.media.video.rememberPosterNavigation
import com.solthariel.pawchan.media.video.rememberVideoPosterStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoPosterStoreRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unreadyOrPipOutputIsNeverCaptured() {
        val engine = PosterTestEngine()
        val player = posterTestSession(false, engine)
        try {
            compose.runOnIdle {
                val store = VideoPosterStore()
                store.capture(player)
                player.mutableVideoOutput.value = true
                player.inPip = true
                store.capture(player)
                assertEquals(0, engine.snapshotReads)
                assertNull(store.frame)
            }
        } finally {
            compose.runOnIdle { player.close() }
        }
    }

    @Test
    fun failedSnapshotRetainsReserveAndClearDoesNotRecycleInFlightDraw() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val engine = PosterTestEngine(bitmap)
        val player = posterTestSession(true, engine)
        try {
            compose.runOnIdle {
                val store = VideoPosterStore()
                store.capture(player)
                assertSame(bitmap, store.frame)
                engine.frame = null
                store.capture(player)
                assertSame(bitmap, store.frame)
                store.clear()
                assertNull(store.frame)
                assertFalse(bitmap.isRecycled)
            }
        } finally {
            compose.runOnIdle { player.close() }
            bitmap.recycle()
        }
    }

    @Test
    fun viewerDisposalClearsReserveAndReopenGetsNewStore() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val player = posterTestSession(true, PosterTestEngine(bitmap))
        val visible = mutableStateOf(true)
        lateinit var active: VideoPosterStore
        lateinit var first: VideoPosterStore
        try {
            compose.setContent {
                if (visible.value) {
                    val store = rememberVideoPosterStore()
                    SideEffect { active = store }
                }
            }
            compose.runOnIdle {
                first = active
                first.capture(player)
                assertSame(bitmap, first.frame)
                visible.value = false
            }
            compose.runOnIdle {
                assertNull(first.frame)
                visible.value = true
            }
            compose.runOnIdle {
                assertNotSame(first, active)
                assertNull(active.frame)
            }
        } finally {
            compose.runOnIdle { player.close() }
            bitmap.recycle()
        }
    }

    @Test
    fun navigationCapturesBeforePageCallbackAndPreservesMetadata() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val player = posterTestSession(true, PosterTestEngine(bitmap))
        val store = VideoPosterStore()
        var nextCalled = false
        var previousCalled = false
        lateinit var wrapped: MediaViewerNavigation
        val navigation =
            MediaViewerNavigation(
                previous = {
                    assertSame(bitmap, store.frame)
                    previousCalled = true
                },
                next = {
                    assertSame(bitmap, store.frame)
                    nextCalled = true
                },
                position = 2,
                total = 3,
            )
        try {
            compose.setContent {
                CompositionLocalProvider(LocalVideoPosterStore provides store) {
                    wrapped = rememberPosterNavigation(player, navigation)
                }
            }
            compose.runOnIdle {
                wrapped.next?.invoke()
                store.clear()
                wrapped.previous?.invoke()
                assertTrue(nextCalled && previousCalled)
                assertEquals(navigation.position, wrapped.position)
                assertEquals(navigation.total, wrapped.total)
            }
        } finally {
            compose.runOnIdle {
                player.close()
                store.clear()
            }
            bitmap.recycle()
        }
    }
}
