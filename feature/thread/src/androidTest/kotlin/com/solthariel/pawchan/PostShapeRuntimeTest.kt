package com.solthariel.pawchan

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class PostShapeRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val fixture = PostShapeFixture(compose)

    @Test fun plainFreshPostFillsItsSquareCorner() = checkCorner(bubble = false, fresh = true)

    @Test fun bubbleFreshPostLeavesItsRoundedCornerClear() = checkCorner(bubble = true, fresh = true)

    @Test fun plainSelectedPostFillsItsSquareCorner() = checkCorner(bubble = false, selected = true)

    @Test fun bubbleSelectedPostLeavesItsRoundedCornerClear() = checkCorner(bubble = true, selected = true)

    @Test fun plainHiddenPostFillsItsSquareCorner() = checkCorner(bubble = false, hidden = true)

    @Test fun bubbleHiddenPostLeavesItsRoundedCornerClear() = checkCorner(bubble = true, hidden = true)

    @Test fun readingMarkerDoesNotOverflowBubbleCorners() = checkCorner(bubble = true, marker = true)

    @Test fun readingMarkerPreservesPlainCorners() = checkCorner(bubble = false, marker = true)

    @Test
    fun changingDisplayModeUpdatesTheFreshOverlayWithoutRecreatingThePost() {
        val bubble = mutableStateOf(true)
        fixture.show(bubble, fresh = mutableStateOf(true))
        assertEquals(Color.Magenta, fixture.pixels()[0, 0])
        compose.runOnIdle { bubble.value = false }
        compose.waitForIdle()
        assertNotEquals(Color.Magenta, fixture.pixels()[0, 0])
        compose.runOnIdle { bubble.value = true }
        compose.waitForIdle()
        assertEquals(Color.Magenta, fixture.pixels()[0, 0])
    }

    @Test
    fun freshFadeChangesOnlyColorAndFinishesAtTheOriginalSurface() {
        val fresh = mutableStateOf(false)
        fixture.show(mutableStateOf(false), fresh)
        val base = fixture.pixels()[0, 0]
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { fresh.value = true }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        val highlighted = fixture.pixels()[0, 0]
        assertNotEquals(base, highlighted)
        compose.runOnIdle { fresh.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(450)
        compose.waitForIdle()
        val fading = fixture.pixels()[0, 0]
        assertNotEquals(base, fading)
        assertNotEquals(highlighted, fading)
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        assertEquals(base, fixture.pixels()[0, 0])
    }

    private fun checkCorner(
        bubble: Boolean,
        fresh: Boolean = false,
        selected: Boolean = false,
        hidden: Boolean = false,
        marker: Boolean = false,
    ) {
        fixture.show(mutableStateOf(bubble), mutableStateOf(fresh), selected, hidden, marker)
        val pixels = fixture.pixels()
        if (bubble) {
            assertEquals(Color.Magenta, pixels[0, 0])
        } else {
            assertNotEquals(Color.Magenta, pixels[0, 0])
            assertEquals(pixels[pixels.width / 2, 0], pixels[0, 0])
        }
        assertNotEquals(Color.Magenta, pixels[pixels.width / 2, pixels.height / 2])
    }
}
