package com.solthariel.pawchan.core.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModalLayerStateTest {
    @Test fun inlinePanelRetainsBothCutoutsRegardlessOfMeasurementOrder() {
        val state = ModalLayerState(true)
        val token = Any()
        state.enter(ModalLayerEntry(token, ModalLayerKind.INLINE_PANEL))
        val toolbar = Rect(0f, 0f, 300f, 56f)
        val panel = Rect(8f, 56f, 292f, 240f)
        state.updateAdditionalPanel(token, toolbar)
        state.updatePanel(token, ModalPanelGeometry(panel, 8f))
        assertEquals(ModalPanelGeometry(panel, 8f, toolbar), state.panel())
        state.updateAdditionalPanel(token, toolbar)
        assertEquals(ModalPanelGeometry(panel, 8f, toolbar), state.panel())
    }

    @Test fun emptyHostHasNoScrim() {
        assertFalse(ModalLayerState(true).dimPage())
    }

    @Test fun firstModalDimsPageAndDoesNotDimItself() {
        val state = ModalLayerState(true)
        val first = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(first)
        assertTrue(state.dimPage())
        assertTrue(state.first(first.token))
        assertFalse(state.covered(first.token))
        assertEquals(0.6f, MODAL_SCRIM_ALPHA)
    }

    @Test fun nestedModalCoversOnlyLowerPlanesAndKeepsOnePageScrim() {
        val state = ModalLayerState(true)
        val first = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        val second = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(first)
        state.enter(second)
        assertTrue(state.dimPage())
        assertTrue(state.covered(first.token))
        assertFalse(state.covered(second.token))
        state.leave(second.token)
        assertTrue(state.dimPage())
        assertFalse(state.covered(first.token))
        state.leave(first.token)
        assertFalse(state.dimPage())
    }

    @Test fun popupDimsPageAndIsCoveredByItsConfirmation() {
        val state = ModalLayerState(true)
        val popup = ModalLayerEntry(Any(), ModalLayerKind.POPUP)
        val confirm = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(popup)
        state.enter(confirm)
        assertTrue(state.dimPage())
        assertTrue(state.covered(popup.token))
        state.leave(confirm.token)
        assertFalse(state.covered(popup.token))
    }

    @Test fun fullScreenOwnsItsBackgroundAndDimsOnlyForAChild() {
        val state = ModalLayerState(true)
        val screen = ModalLayerEntry(Any(), ModalLayerKind.FULL_SCREEN)
        val menu = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(screen)
        assertFalse(state.dimPage())
        assertFalse(state.covered(screen.token))
        state.enter(menu)
        assertFalse(state.dimPage())
        assertTrue(state.covered(screen.token))
        assertFalse(state.covered(menu.token))
    }

    @Test fun opaqueViewerCoversTheParentWithoutDimmingItAndStillDimsForItsOwnChild() {
        val state = ModalLayerState(true)
        val composer = ModalLayerEntry(Any(), ModalLayerKind.FULL_SCREEN)
        val viewer = ModalLayerEntry(Any(), ModalLayerKind.FULL_SCREEN, ModalScrimStrength.NONE)
        val dialog = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(composer)
        state.enter(viewer)
        assertTrue(state.covered(composer.token))
        assertEquals(0f, state.coveredAlpha(composer.token), 0f)
        state.enter(dialog)
        assertEquals(0.6f, state.coveredAlpha(viewer.token), 0f)
        state.leave(dialog.token)
        state.leave(viewer.token)
        assertFalse(state.covered(composer.token))
        assertEquals(0f, state.coveredAlpha(composer.token), 0f)
    }

    @Test fun inlinePreviewOwnsOneBackdropEvenWhenAWindowOpensAboveIt() {
        val state = ModalLayerState(true)
        val preview = ModalLayerEntry(Any(), ModalLayerKind.INLINE)
        val menu = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(preview)
        state.enter(menu)
        assertTrue(state.first(preview.token))
        assertFalse(state.dimPage())
        assertTrue(state.covered(preview.token))
    }

    @Test fun removingUnderlyingOwnerDoesNotRemoveItsIndependentChild() {
        val state = ModalLayerState(true)
        val first = ModalLayerEntry(Any(), ModalLayerKind.POPUP)
        val child = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(first)
        state.enter(child)
        state.leave(first.token)
        assertTrue(state.first(child.token))
        assertTrue(state.dimPage())
        assertFalse(state.covered(child.token))
        state.leave(first.token)
        state.leave(child.token)
        assertFalse(state.dimPage())
    }

    @Test fun hostsDoNotShareState() {
        val first = ModalLayerState(true)
        val second = ModalLayerState(true)
        first.enter(ModalLayerEntry(Any(), ModalLayerKind.WINDOW))
        assertTrue(first.dimPage())
        assertFalse(second.dimPage())
    }

    @Test(expected = IllegalStateException::class)
    fun duplicateOwnerCannotBeRegisteredTwice() {
        val state = ModalLayerState(true)
        val entry = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(entry)
        state.enter(entry)
    }

    @Test fun unknownOwnerIsNotCoveredAndRemovalIsHarmless() {
        val state = ModalLayerState(false)
        val unknown = Any()
        assertFalse(state.covered(unknown))
        assertFalse(state.first(unknown))
        state.leave(unknown)
        assertFalse(state.dimPage())
    }
}
