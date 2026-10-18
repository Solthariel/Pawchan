package com.solthariel.pawchan

import com.solthariel.pawchan.thread.PreviewSwipeAction
import com.solthariel.pawchan.thread.previewSwipeAction
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSwipeDecisionTest {
    @Test fun shortSlowDragRestoresWindowInEitherDirection() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(20f, 500f, 0f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(-20f, 500f, 0f))
    }

    @Test fun upwardsClosesOneAndDownwardsClosesTheChain() {
        assertEquals(PreviewSwipeAction.POP, previewSwipeAction(-160f, 500f, 0f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(160f, 500f, 0f))
    }

    @Test fun fastIntentionalFlickNeedsDistanceAndMatchingDirection() {
        assertEquals(PreviewSwipeAction.POP, previewSwipeAction(-30f, 500f, -1500f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(30f, 500f, 1500f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(30f, 500f, -1500f))
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(5f, 500f, 9000f))
    }

    @Test fun smallAndLargeWindowsUseBoundedThresholds() {
        assertEquals(PreviewSwipeAction.RESTORE, previewSwipeAction(40f, 50f, 0f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(50f, 50f, 0f))
        assertEquals(PreviewSwipeAction.CLEAR, previewSwipeAction(150f, 2000f, 0f))
    }
}
