package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.sourceToCanvas
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageEditMatrixTest {
    @Test
    fun imageAndMarkTransformsMatchForEveryRotationAndReflection() {
        val bounds = ImageEditBounds(10, 20, 90, 60)
        val points = listOf(DraftPoint(.1f, .2f), DraftPoint(.9f, .6f), DraftPoint(.31f, .44f))
        for (turns in 0..3) {
            for (mirror in listOf(false, true)) {
                val edit = DraftImageEdit(DraftCrop(.1f, .2f, .9f, .6f), turns, mirror)
                val coefficients = imageEditMatrixValues(bounds, edit)
                val space = ImageEditRasterSpace(100, 100, bounds, edit)
                val outputWidth = if (turns % 2 == 0) 80 else 40
                val outputHeight = if (turns % 2 == 0) 40 else 80
                points.forEach { point ->
                    val localX = point.x * 100 - bounds.left
                    val localY = point.y * 100 - bounds.top
                    val x = coefficients[0] * localX + coefficients[1] * localY + coefficients[2]
                    val y = coefficients[3] * localX + coefficients[4] * localY + coefficients[5]
                    val mapped = space.map(point)
                    val normalized = edit.sourceToCanvas(point)
                    assertEquals(x, mapped.x, .0001f)
                    assertEquals(y, mapped.y, .0001f)
                    assertEquals(normalized.x * outputWidth, x, .0001f)
                    assertEquals(normalized.y * outputHeight, y, .0001f)
                }
            }
        }
    }
}
