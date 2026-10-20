package com.solthariel.pawchan.settings

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterEditorActionsTest {
    @Test fun regularTextFitsOneRowWithoutShrinkingTouchTargets() {
        assertTrue(filterEditorActionsFit(312.dp, 64.dp, 42.dp, 68.dp, false))
        assertTrue(filterEditorActionsFit(328.dp, 64.dp, 42.dp, 68.dp, true))
    }

    @Test fun narrowOrLargeFontLayoutFallsBackInsteadOfClippingActions() {
        assertFalse(filterEditorActionsFit(240.dp, 64.dp, 42.dp, 68.dp, false))
        assertFalse(filterEditorActionsFit(312.dp, 128.dp, 84.dp, 136.dp, false))
        assertFalse(filterEditorActionsFit(280.dp, 64.dp, 42.dp, 68.dp, true))
    }
}
