package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowBottomOcclusionTest {
    @Test fun keyboardAndNavigationNeverDoubleCountTheCoveredArea() {
        assertEquals(300, windowBottomOcclusion(48, 300))
        assertEquals(300, windowBottomOcclusion(0, 300))
        assertEquals(48, windowBottomOcclusion(48, 0))
        assertEquals(0, windowBottomOcclusion(0, 0))
    }
}
