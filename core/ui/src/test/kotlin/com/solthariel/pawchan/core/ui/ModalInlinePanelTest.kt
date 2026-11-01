package com.solthariel.pawchan.core.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModalInlinePanelTest {
    @Test fun measuredPanelKeepsOneNormalBackdropAndClearsOnDismiss() {
        val state = ModalLayerState(true)
        val panel = ModalLayerEntry(Any(), ModalLayerKind.INLINE_PANEL)
        state.enter(panel)
        assertEquals(0.6f, state.pageAlpha(), 0f)
        assertEquals(0.6f, state.systemBarAlpha(), 0f)
        val geometry = ModalPanelGeometry(Rect(8f, 54f, 352f, 220f), 8f)
        state.updatePanel(panel.token, geometry)
        state.updatePanel(panel.token, geometry)
        assertEquals(geometry, state.panel())
        state.enter(ModalLayerEntry(Any(), ModalLayerKind.WINDOW))
        assertEquals(geometry, state.panel())
        assertEquals(0.6f, state.coveredAlpha(panel.token), 0f)
        assertEquals(0.6f, state.pageAlpha(), 0f)
        state.leave(panel.token)
        assertNull(state.panel())
    }

    @Test fun panelGeometryNeverLeaksToAnotherHostOrOwner() {
        val first = ModalLayerState(true)
        val second = ModalLayerState(true)
        val geometry = ModalPanelGeometry(Rect(8f, 54f, 352f, 220f), 8f)
        first.updatePanel(Any(), geometry)
        assertNull(first.panel())
        assertNull(second.panel())
        val ordinary = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        first.enter(ordinary)
        first.updatePanel(ordinary.token, geometry)
        assertNull(first.panel())
    }
}
