package com.solthariel.pawchan

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.solthariel.pawchan.media.video.LocalVideoPosterStore
import com.solthariel.pawchan.media.video.VideoPosterFallback
import com.solthariel.pawchan.media.video.VideoPosterStore
import com.solthariel.pawchan.media.video.rememberVideoPosterReserve
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoPosterReserveRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun incomingReserveSurvivesBlackCaptureAndChangesOnlyWithOwner() {
        val outgoing = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val incoming = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val engine = PosterTestEngine(outgoing)
        val player = posterTestSession(true, engine)
        val store = VideoPosterStore()
        val owner = mutableStateOf(Any())
        lateinit var reserve: Bitmap
        try {
            compose.runOnIdle { store.capture(player) }
            compose.setContent {
                CompositionLocalProvider(LocalVideoPosterStore provides store) {
                    val frozen = rememberVideoPosterReserve(owner.value)
                    SideEffect { reserve = requireNotNull(frozen) }
                    Box(Modifier.testTag("reserve")) {
                        VideoPosterFallback(frozen)
                    }
                }
            }
            compose.runOnIdle {
                engine.frame = incoming
                store.capture(player)
                assertSame(incoming, store.frame)
            }
            compose.runOnIdle { assertSame(outgoing, reserve) }
            val pixels = compose.onNodeWithTag("reserve").captureToImage().toPixelMap()
            assertTrue(pixels[pixels.width / 2, pixels.height / 2].red > 0.9f)
            compose.runOnIdle { owner.value = Any() }
            compose.runOnIdle { assertSame(incoming, reserve) }
        } finally {
            compose.runOnIdle {
                player.close()
                store.clear()
            }
            outgoing.recycle()
            incoming.recycle()
        }
    }

    @Test
    fun emptyReserveDoesNotSwitchToIncomingSnapshotMidHandoff() {
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        val player = posterTestSession(true, PosterTestEngine(bitmap))
        val store = VideoPosterStore()
        var reserve: Bitmap? = bitmap
        try {
            compose.setContent {
                CompositionLocalProvider(LocalVideoPosterStore provides store) {
                    val frozen = rememberVideoPosterReserve(player)
                    SideEffect { reserve = frozen }
                }
            }
            compose.runOnIdle {
                assertNull(reserve)
                store.capture(player)
            }
            compose.runOnIdle { assertNull(reserve) }
        } finally {
            compose.runOnIdle {
                player.close()
                store.clear()
            }
            bitmap.recycle()
        }
    }
}
