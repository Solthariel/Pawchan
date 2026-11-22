package com.solthariel.pawchan.core.ui

import androidx.compose.ui.graphics.Color
import com.solthariel.pawchan.core.ui.PullRefreshColors
import com.solthariel.pawchan.core.ui.PullRefreshSide
import com.solthariel.pawchan.core.ui.PullRefreshStyle
import com.solthariel.pawchan.core.ui.pullRefreshCenterY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullRefreshGeometryTest {
    private val style = PullRefreshStyle(20f, 64f, 8.75f, PullRefreshColors(Color.Black, Color.White), 42f)

    @Test
    fun discStartsBeyondTheTopEdgeAndFullyEntersAtTheThreshold() {
        assertEquals(-20f, pullRefreshCenterY(0f, 1_000f, style, PullRefreshSide.TOP), 0f)
        val center = pullRefreshCenterY(1f, 1_000f, style, PullRefreshSide.TOP)
        assertEquals(44f, center, 0f)
        assertTrue(center - style.radius >= 0f)
        assertEquals(64f, center + style.radius, 0f)
    }

    @Test
    fun bottomDiscIsMirroredAboveTheFloatingNavigationBar() {
        val center = pullRefreshCenterY(1f, 1_000f, style, PullRefreshSide.BOTTOM)
        assertEquals(914f, center, 0f)
        assertTrue(center + style.radius < 1_000f - style.bottomInset)
    }

    @Test
    fun overpullStaysAtTheLoadingPositionOnBothEdges() {
        for (side in PullRefreshSide.entries) {
            val loadingCenter = pullRefreshCenterY(1f, 1_000f, style, side)
            assertEquals(loadingCenter, pullRefreshCenterY(1.25f, 1_000f, style, side), 0f)
            assertEquals(loadingCenter, pullRefreshCenterY(2f, 1_000f, style, side), 0f)
        }
    }
}
