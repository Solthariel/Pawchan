package com.solthariel.pawchan.composer

import androidx.compose.ui.geometry.Offset
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.sourceToCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageMarkPointTest {
    @Test
    fun outputTouchMapsToSourceForAllOrientations() {
        for (turn in 0..3) {
            for (mirrored in listOf(false, true)) {
                val edit = DraftImageEdit(DraftCrop(.1f, .2f, .9f, .8f), turn, mirrored)
                val point = requireNotNull(imageMarkPoint(edit, Offset(60f, 70f), 200f, 100f))
                val restored = edit.sourceToCanvas(point)
                assertEquals(.3f, restored.x, .00001f)
                assertEquals(.7f, restored.y, .00001f)
            }
        }
    }

    @Test
    fun outsideAndInvalidTouchesCannotProduceInvalidPoints() {
        val edit = DraftImageEdit()
        val point = requireNotNull(imageMarkPoint(edit, Offset(-20f, 300f), 100f, 100f))
        assertEquals(0f, point.x, .00001f)
        assertEquals(1f, point.y, .00001f)
        assertNull(imageMarkPoint(edit, Offset(Float.NaN, 1f), 100f, 100f))
        assertNull(imageMarkPoint(edit, Offset.Zero, 0f, 100f))
    }
}
