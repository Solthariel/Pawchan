package com.solthariel.pawchan.navigation.chrome

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarAppearanceTest {
    @Test fun transparencyUsesPercentNotOpacity() {
        assertEquals(1f, bottomBarSurfaceAlpha(0), 0.0001f)
        assertEquals(0.8f, bottomBarSurfaceAlpha(20), 0.0001f)
        assertEquals(0f, bottomBarSurfaceAlpha(100), 0.0001f)
    }

    @Test fun invalidValuesCannotProduceAnInvalidAlpha() {
        assertEquals(1f, bottomBarSurfaceAlpha(-1), 0.0001f)
        assertEquals(0f, bottomBarSurfaceAlpha(101), 0.0001f)
    }
}
