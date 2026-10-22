package com.solthariel.pawchan

import com.solthariel.pawchan.thread.PreviewSwipeAction
import com.solthariel.pawchan.thread.previewLayerShift
import com.solthariel.pawchan.thread.previewSwipeAction
import com.solthariel.pawchan.thread.previewSwipeDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSwipeDecisionTest {
    @Test fun slowLargeCardRequiresHalfViewport() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(349f, 700f, 0f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(350f, 700f, 0f))
    }

    @Test fun smallCardRequiresQuarterViewport() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(174f, 700f, 0f, 100f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(175f, 700f, 0f, 100f))
    }

    @Test fun mediumCardInterpolatesThreshold() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(232f, 700f, 0f, 350f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(234f, 700f, 0f, 350f))
    }

    @Test fun forwardVelocitySmoothlyLowersThreshold() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(100f, 700f, 0f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(100f, 700f, 700f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(100f, 700f, 6_300f))
    }

    @Test fun reverseVelocityCannotAssistClosing() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(100f, 700f, -50_000f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(-100f, 700f, 50_000f))
    }

    @Test fun directionAndBoundaryRemainDeterministic() {
        assertEquals(PreviewSwipeAction.POP, previewSwipeAction(-350f, 700f, 0f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(0f, 700f, 100_000f))
    }

    @Test fun viewportAndSpeedScalingPreservesDecision() {
        assertEquals(previewSwipeAction(100f, 700f, 6_300f), previewSwipeAction(200f, 1_400f, 12_600f))
    }

    @Test fun timingIsBoundedAndDirectionIndependent() {
        assertEquals(512, previewSwipeDuration(1_000f, 0f))
        assertEquals(150, previewSwipeDuration(1f, 100_000f))
        assertEquals(previewSwipeDuration(500f, 3_000f), previewSwipeDuration(-500f, -3_000f))
    }

    @Test fun downwardPullSharesMovementAcrossStack() {
        assertEquals(120f, previewLayerShift(120f, 0, 3), 0f)
        assertEquals(80f, previewLayerShift(120f, 1, 3), 0f)
        assertEquals(40f, previewLayerShift(120f, 2, 3), 0f)
    }

    @Test fun upwardPullOnlyMovesTheActiveWindow() {
        assertEquals(-120f, previewLayerShift(-120f, 0, 3), 0f)
        assertEquals(0f, previewLayerShift(-120f, 1, 3), 0f)
    }
}
