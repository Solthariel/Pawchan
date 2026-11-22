package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePreviewTransformTest {
    @Test fun relativeTransformMatchesEveryPairOfOrientations() {
        val orientations =
            (0..3).flatMap { turn ->
                listOf(false, true).map { DraftImageEdit(quarterTurns = turn, mirrored = it) }
            }
        for (base in orientations) {
            for (target in orientations) {
                checkTransform(base, target)
            }
        }
    }

    private fun checkTransform(
        base: DraftImageEdit,
        target: DraftImageEdit,
    ) {
        val delta = requireNotNull(imagePreviewTransform(base, target))
        for (point in listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)) {
            val first = map(point, base.quarterTurns, base.mirrored)
            assertEquals(
                map(point, target.quarterTurns, target.mirrored),
                map(first, delta.quarterTurns, delta.mirrored),
            )
        }
    }

    @Test fun changedCropNeverReusesOldPixels() {
        val target = DraftImageEdit(crop = DraftCrop(.1f, .1f, .9f, .9f), quarterTurns = 1)
        assertNull(imagePreviewTransform(DraftImageEdit(), target))
    }

    @Test fun addedRedactionNeverUsesAnUnredactedFrame() {
        val redaction =
            DraftRedaction(
                listOf(DraftPoint(.2f, .2f), DraftPoint(.8f, .8f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val edited = DraftImageEdit(quarterTurns = 1, marks = listOf(redaction))
        assertNull(imagePreviewTransform(DraftImageEdit(), edited))
    }

    private fun map(
        point: Pair<Int, Int>,
        turns: Int,
        mirrored: Boolean,
    ): Pair<Int, Int> {
        var x = if (mirrored) 1 - point.first else point.first
        var y = point.second
        repeat(turns) {
            val oldX = x
            x = 1 - y
            y = oldX
        }
        return x to y
    }
}
