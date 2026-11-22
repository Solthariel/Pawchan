package com.solthariel.pawchan.core.ui

import com.solthariel.pawchan.core.ui.PullRefreshGesture
import com.solthariel.pawchan.core.ui.PullRefreshReleaseMode
import com.solthariel.pawchan.core.ui.PullRefreshSide
import com.solthariel.pawchan.core.ui.pullRefreshThreshold
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PullRefreshGestureTest {
    @Test
    fun topEdgeRefreshIsArmedAtOneQuarterOfPhoneViewport() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        assertEquals(100f, gesture.threshold, 0f)
        assertEquals(100f, gesture.consumeEdge(100f, allowBottom = true), 0f)
        assertTrue(gesture.release(busy = false))
        assertEquals(PullRefreshSide.TOP, gesture.side)
        assertEquals(PullRefreshReleaseMode.LOAD, gesture.release.mode)
        assertEquals(0f, gesture.distance, 0f)
        assertTrue(gesture.requested)
        assertFalse(gesture.release(busy = true))
        assertEquals(1, gesture.requestId)
        gesture.complete()
        assertFalse(gesture.requested)
    }

    @Test
    fun bottomEdgeUsesTheSameThresholdAndCannotDoubleSubmit() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        gesture.consumeEdge(-100f, allowBottom = true)
        assertTrue(gesture.release(busy = false))
        assertEquals(PullRefreshSide.BOTTOM, gesture.side)
        assertFalse(gesture.release(busy = true))
        assertEquals(1, gesture.requestId)
    }

    @Test
    fun shortPullFoldsBackWithoutRefreshing() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        gesture.consumeEdge(90f, allowBottom = true)
        assertFalse(gesture.release(busy = false))
        assertEquals(PullRefreshReleaseMode.RETRACT, gesture.release.mode)
        assertEquals(0.9f, gesture.release.strain, 0.0001f)
        assertEquals(0, gesture.requestId)
    }

    @Test
    fun reverseMotionReturnsUnusedPixelsToTheList() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        gesture.consumeEdge(100f, allowBottom = true)
        assertEquals(-100f, gesture.consumeClosing(-150f), 0f)
        assertEquals(0f, gesture.distance, 0f)
        assertFalse(gesture.release(busy = false))
    }

    @Test
    fun disabledBottomAndNonEdgeMotionCannotArmRefresh() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        assertEquals(0f, gesture.consumeEdge(-100f, allowBottom = false), 0f)
        assertEquals(0f, gesture.consumeEdge(0f, allowBottom = true), 0f)
        assertFalse(gesture.release(busy = false))
    }

    @Test
    fun overpullIsClampedButAllEdgeMotionIsConsumedWithoutNativeStretch() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        assertEquals(1_000f, gesture.consumeEdge(1_000f, allowBottom = true), 0f)
        assertEquals(200f, gesture.distance, 0f)
        assertEquals(2f, gesture.strain, 0f)
    }

    @Test
    fun tabletThresholdAndInitialSizeAreFinite() {
        assertEquals(160f, pullRefreshThreshold(960f, tablet = true, density = 2f), 0f)
        assertEquals(192f, pullRefreshThreshold(960f, tablet = false, density = 2f), 0f)
        assertEquals(1f, pullRefreshThreshold(0f, tablet = false, density = 1f), 0f)
    }

    @Test
    fun tallViewportUsesShorterPullThreshold() {
        assertEquals(96f, pullRefreshThreshold(800f, tablet = false, density = 1f), 0f)
        assertEquals(96f, pullRefreshThreshold(1_200f, tablet = true, density = 1f), 0f)
        assertEquals(192f, pullRefreshThreshold(1_600f, tablet = false, density = 2f), 0f)
        assertEquals(30f, pullRefreshThreshold(120f, tablet = false, density = 1f), 0f)
    }

    @Test
    fun externalRefreshPreventsAnotherEdgeRelease() {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 2f)
        gesture.consumeEdge(100f, allowBottom = true)
        assertFalse(gesture.release(busy = true))
        assertFalse(gesture.requested)
        assertEquals(0, gesture.requestId)
    }
}
