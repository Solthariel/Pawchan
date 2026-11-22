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

    @Test fun plainFreshPostFillsItsSquareCorner() = checkCorner(fresh = true)

    @Test fun plainSelectedPostFillsItsSquareCorner() = checkCorner(selected = true)

    @Test fun plainHiddenPostFillsItsSquareCorner() = checkCorner(hidden = true)

    @Test fun readingMarkerPreservesPlainCorners() = checkCorner(marker = true)

    @Test
    fun freshFadeChangesOnlyColorAndFinishesAtTheOriginalSurface() {
        val fresh = mutableStateOf(false)
        fixture.show(fresh)
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
        fresh: Boolean = false,
        selected: Boolean = false,
        hidden: Boolean = false,
        marker: Boolean = false,
    ) {
        fixture.show(mutableStateOf(fresh), selected, hidden, marker)
        val pixels = fixture.pixels()
        assertNotEquals(Color.Magenta, pixels[0, 0])
        assertEquals(pixels[pixels.width / 2, 0], pixels[0, 0])
        assertNotEquals(Color.Magenta, pixels[pixels.width / 2, pixels.height / 2])
    }
}
