package com.solthariel.pawchan

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.rememberVideoOutput
import com.solthariel.pawchan.media.video.rememberVideoPosterAlpha
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoPosterHandoffRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun renderedVideoToUnreadySessionHasOpaquePosterOnFirstComposition() = verifyReplacement(false)

    @Test
    fun anotherReadyOwnerAlsoGetsItsOwnPosterAnimation() = verifyReplacement(true)

    @Test
    fun outputResetIsOpaqueBeforeTheResetEffectRuns() {
        val player = posterTestSession(true)
        val records = ArrayList<Pair<Boolean, Float>>()
        try {
            observe(player, records)
            compose.mainClock.advanceTimeBy(FADE_SETTLE_MS)
            compose.runOnIdle {
                assertEquals(0f, records.last().second)
                records.clear()
                player.mutableVideoOutput.value = false
            }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                assertTrue(records.isNotEmpty())
                assertTrue(records.all { !it.first && it.second == 1f })
            }
        } finally {
            compose.runOnIdle { player.close() }
        }
    }

    private fun verifyReplacement(ready: Boolean) {
        val old = posterTestSession(true)
        val next = posterTestSession(ready)
        val current = mutableStateOf(old)
        val records = ArrayList<Pair<Boolean, Float>>()
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent {
                val player = current.value
                val output by rememberVideoOutput(player)
                val alpha by rememberVideoPosterAlpha(output, player)
                val record = output to alpha
                SideEffect { records += record }
            }
            compose.mainClock.advanceTimeBy(FADE_SETTLE_MS)
            compose.runOnIdle {
                assertEquals(true to 0f, records.last())
                records.clear()
                current.value = next
            }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                assertTrue(records.isNotEmpty())
                assertEquals(ready to 1f, records.first())
                if (!ready) assertFalse(records.any { it.first })
            }
        } finally {
            compose.runOnIdle {
                old.close()
                next.close()
            }
        }
    }

    private fun observe(
        player: VideoPlaybackSession,
        records: MutableList<Pair<Boolean, Float>>,
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val output by rememberVideoOutput(player)
            val alpha by rememberVideoPosterAlpha(output, player)
            val record = output to alpha
            SideEffect { records += record }
        }
    }
}

private const val FADE_SETTLE_MS = 250L
