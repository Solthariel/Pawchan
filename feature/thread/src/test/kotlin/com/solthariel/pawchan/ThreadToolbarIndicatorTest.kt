package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.thread.threadToolbarActive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadToolbarIndicatorTest {
    @Test fun unreadHighlightsOnlyNavigationKinds() {
        for (kind in UiIconKind.entries) {
            if (kind == UiIconKind.BACK || kind == UiIconKind.MENU) {
                assertTrue(threadToolbarActive(kind, false, true))
            } else {
                assertFalse(threadToolbarActive(kind, false, true))
            }
        }
    }

    @Test fun explicitActiveStateIsNeverClearedByUnreadProjection() {
        for (kind in UiIconKind.entries) assertTrue(threadToolbarActive(kind, true, false))
    }

    @Test fun clearingUnreadDoesNotLeaveTheBackButtonActive() {
        assertTrue(threadToolbarActive(UiIconKind.BACK, false, true))
        assertFalse(threadToolbarActive(UiIconKind.BACK, false, false))
    }
}
