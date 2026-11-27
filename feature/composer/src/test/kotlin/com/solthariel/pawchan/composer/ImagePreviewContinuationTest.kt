package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePreviewContinuationTest {
    private val pen = DraftStroke(listOf(DraftPoint(.2f, .3f)), -1, .1f)
    private val mask =
        DraftRedaction(listOf(DraftPoint(.6f, .7f)), DraftRedactionMode.PIXELS, DraftRedactionTool.BRUSH, .1f)

    @Test fun retainsEveryPendingStrokeAcrossCommitAndNextGesture() {
        val base = DraftImageEdit(marks = listOf(mask))
        assertEquals(emptyList<Any>(), imagePreviewContinuation(base, base))
        assertEquals(listOf(pen, mask), imagePreviewContinuation(base, base.copy(marks = listOf(mask, pen, mask))))
    }

    @Test fun neverReusesPixelsWhenAnExistingRedactionIsMovedOrRemoved() {
        val base = DraftImageEdit(marks = listOf(mask))
        assertNull(imagePreviewContinuation(base, DraftImageEdit()))
        val moved = mask.copy(points = listOf(DraftPoint(.1f, .1f)))
        assertNull(imagePreviewContinuation(base, base.copy(marks = listOf(moved))))
    }

    @Test fun geometryChangesRequireTheirOwnCompletedRaster() {
        val base = DraftImageEdit(marks = listOf(mask))
        assertNull(imagePreviewContinuation(base, base.rotateLeft()))
        assertNull(imagePreviewContinuation(base, base.flipHorizontal()))
        assertNull(imagePreviewContinuation(base, base.copy(crop = DraftCrop(.1f, .1f, .9f, .9f))))
    }

    @Test fun alteredMarkerIsNotPaintedTwiceOverOldPixels() {
        val base = DraftImageEdit(marks = listOf(pen))
        assertNull(imagePreviewContinuation(base, base.copy(marks = listOf(pen.copy(marker = true)))))
    }

    @Test fun everyVisibleToolSharesTheFullPlaneWithoutChangingTheSavedCrop() {
        val edit = DraftImageEdit(crop = DraftCrop(.1f, .2f, .6f, .9f), marks = listOf(mask))
        assertEquals(edit.copy(crop = DraftCrop()), imageEditorDisplayEdit(edit))
        assertEquals(DraftCrop(.1f, .2f, .6f, .9f), edit.crop)
    }
}
