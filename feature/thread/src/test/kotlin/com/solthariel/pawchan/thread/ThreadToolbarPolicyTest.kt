package com.solthariel.pawchan.thread

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadToolbarPolicyTest {
    @Test fun compactActionsAreUsedBeforeTheSixTargetsWouldCollide() {
        for (width in listOf(0, 240, 288, 310, 311)) assertTrue(threadToolbarUsesCompactActions(width.dp))
    }

    @Test fun fullActionsRetain48DpTargetsAtTheBoundaryAndOnWideScreens() {
        for (width in listOf(312, 320, 360, 393, 480, 720)) assertFalse(threadToolbarUsesCompactActions(width.dp))
    }
}
