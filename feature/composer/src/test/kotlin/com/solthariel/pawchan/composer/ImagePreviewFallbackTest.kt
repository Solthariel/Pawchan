package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePreviewFallbackTest {
    private val mask =
        DraftRedaction(listOf(DraftPoint(.5f, .5f)), DraftRedactionMode.FILL, DraftRedactionTool.BRUSH, .1f)
    private val pen = DraftStroke(listOf(DraftPoint(.2f, .2f)), -1, .1f)

    @Test fun undoEraseAndResetRetainAnOverCoveredFrameUntilTheNewRasterArrives() {
        val base = DraftImageEdit(marks = listOf(mask, pen))
        assertEquals(listOf(mask), imagePreviewFallback(base, base.copy(marks = listOf(mask))))
        assertEquals(emptyList<Any>(), imagePreviewFallback(base, DraftImageEdit()))
    }

    @Test fun aMovedOrNewMaskIsAlwaysCoveredEvenWhenHistoryIsNotAPrefix() {
        val moved = mask.copy(points = listOf(DraftPoint(.8f, .8f)))
        val base = DraftImageEdit(marks = listOf(mask))
        assertEquals(listOf(moved), imagePreviewFallback(base, base.copy(marks = listOf(moved))))
        assertNotNull(imagePreviewFallbackTransform(base, base.copy(marks = listOf(moved)).rotateLeft()))
    }

    @Test fun neverReusesAnotherCroppedPlane() {
        val base = DraftImageEdit(marks = listOf(mask))
        val target = base.copy(crop = DraftCrop(.1f, .1f, .9f, .9f))
        assertNull(imagePreviewFallback(base, target))
        assertNull(imagePreviewFallbackTransform(base, target))
    }
}
