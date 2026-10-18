package com.solthariel.pawchan.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationMotionTest {
    @Test fun rootTabsUseFade() {
        assertTrue(usesTabMotion(AppRoute.CATALOG, AppRoute.SETTINGS, false))
        assertTrue(usesTabMotion(AppRoute.BOARDS, AppRoute.LIBRARY, false))
    }

    @Test fun explicitBottomClickFromADeepPageStillUsesTabMotion() {
        assertTrue(usesTabMotion(AppRoute.THREAD, AppRoute.SETTINGS, true))
        assertTrue(usesTabMotion(AppRoute.STATISTICS, AppRoute.LIBRARY, true))
    }

    @Test fun ordinaryBackAndNestedScreensUseReversePageMotion() {
        assertFalse(usesTabMotion(AppRoute.THREAD, AppRoute.CATALOG, false))
        assertFalse(usesTabMotion(AppRoute.SETTINGS, AppRoute.STATISTICS, false))
        assertFalse(usesTabMotion(AppRoute.HISTORY, AppRoute.LIBRARY, false))
        assertFalse(usesTabMotion(null, AppRoute.CATALOG, false))
    }
}
