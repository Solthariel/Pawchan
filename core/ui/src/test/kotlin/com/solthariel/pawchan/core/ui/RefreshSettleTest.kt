package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshSettleTest {
    @Test fun completedRotationDoesNotStartAnotherAnimation() {
        assertNull(refreshSettleDuration(360f))
    }

    @Test fun tinyRemainingDistanceKeepsTheExistingMinimumThreshold() {
        assertNull(refreshSettleDuration(0.49f))
        assertEquals(1, refreshSettleDuration(0.5f))
    }

    @Test fun settlementUsesTheSameRotationSpeedAndRounding() {
        assertEquals(425, refreshSettleDuration(180f))
        assertEquals(848, refreshSettleDuration(359f))
    }
}
