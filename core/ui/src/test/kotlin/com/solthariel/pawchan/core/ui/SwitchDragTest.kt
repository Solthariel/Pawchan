package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchDragTest {
    @Test fun dragChangesStateOnlyAfterCrossingTheMiddle() {
        assertFalse(switchDragChecked(0f))
        assertFalse(switchDragChecked(.49f))
        assertTrue(switchDragChecked(.5f))
        assertTrue(switchDragChecked(1f))
    }
}
