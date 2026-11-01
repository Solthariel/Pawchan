package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ModalScrimStrengthTest {
    @Test fun normalMatchesInheritedMaterialThemeDim() {
        assertEquals(0.6f, ModalScrimStrength.STANDARD.alpha(), 0.00001f)
    }

    @Test fun mediaUsesUserSelectedNinetyPercent() {
        assertEquals(0.9f, ModalScrimStrength.MEDIA.alpha(), 0.00001f)
    }

    @Test fun strongFirstModalKeepsPageOpacityWhileNormalChildOpens() {
        val state = ModalLayerState(true)
        val picker = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
        val child = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        state.enter(picker)
        assertEquals(0.9f, state.pageAlpha(), 0.00001f)
        state.enter(child)
        assertEquals(0.9f, state.pageAlpha(), 0.00001f)
        assertEquals(0.6f, state.coveredAlpha(picker.token), 0.00001f)
        assertEquals(0f, state.coveredAlpha(child.token), 0f)
        state.leave(child.token)
        assertEquals(0f, state.coveredAlpha(picker.token), 0f)
        assertEquals(0.9f, state.pageAlpha(), 0.00001f)
        state.leave(picker.token)
        assertEquals(0f, state.pageAlpha(), 0f)
    }

    @Test fun childStrengthAffectsOnlyTheImmediatelyLowerPlane() {
        val state = ModalLayerState(true)
        val parent = ModalLayerEntry(Any(), ModalLayerKind.WINDOW)
        val media = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
        val child = ModalLayerEntry(Any(), ModalLayerKind.POPUP)
        state.enter(parent)
        state.enter(media)
        state.enter(child)
        assertEquals(0.6f, state.pageAlpha(), 0f)
        assertEquals(0.9f, state.coveredAlpha(parent.token), 0.00001f)
        assertEquals(0.6f, state.coveredAlpha(media.token), 0f)
        state.leave(media.token)
        assertEquals(0.6f, state.coveredAlpha(parent.token), 0f)
    }

    @Test fun fullScreenAndInlineDoNotAcquireExtraPageScrim() {
        for (kind in listOf(ModalLayerKind.FULL_SCREEN, ModalLayerKind.INLINE)) {
            val state = ModalLayerState(true)
            val background = ModalLayerEntry(Any(), kind)
            val picker = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
            state.enter(background)
            state.enter(picker)
            assertEquals(0f, state.pageAlpha(), 0f)
            assertEquals(0.9f, state.coveredAlpha(background.token), 0.00001f)
        }
    }

    @Test fun removingMediaPromotesNormalChildAndUnknownOwnerDoesNotDim() {
        val state = ModalLayerState(true)
        val media = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
        state.enter(media)
        state.enter(ModalLayerEntry(Any(), ModalLayerKind.WINDOW))
        state.leave(media.token)
        assertEquals(0.6f, state.pageAlpha(), 0f)
        assertEquals(0f, state.coveredAlpha(Any()), 0f)
    }

    @Test fun systemBarsFollowBasePlaneAndDoNotMultiplyForAChild() {
        val state = ModalLayerState(true)
        assertEquals(0f, state.systemBarAlpha(), 0f)
        val picker = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
        state.enter(picker)
        state.enter(ModalLayerEntry(Any(), ModalLayerKind.WINDOW))
        assertEquals(0.9f, state.systemBarAlpha(), 0f)
        state.leave(picker.token)
        assertEquals(0.6f, state.systemBarAlpha(), 0f)
    }

    @Test fun fullScreenBarsDimOnlyWhenChildIsPresentAndInlineBarsFollowItsBackdrop() {
        val state = ModalLayerState(true)
        val full = ModalLayerEntry(Any(), ModalLayerKind.FULL_SCREEN)
        val child = ModalLayerEntry(Any(), ModalLayerKind.WINDOW, ModalScrimStrength.MEDIA)
        state.enter(full)
        assertEquals(0f, state.systemBarAlpha(), 0f)
        state.enter(child)
        assertEquals(0.9f, state.systemBarAlpha(), 0f)
        state.leave(child.token)
        state.leave(full.token)
        state.enter(ModalLayerEntry(Any(), ModalLayerKind.INLINE))
        assertEquals(0.6f, state.systemBarAlpha(), 0f)
    }
}
