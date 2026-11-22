package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RedactionColorRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exportBurnsTheChosenOpaqueColorIntoBothBrushAndRectangle() {
        DraftRedactionTool.entries.forEach { tool ->
            val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            val mark = DraftRedaction(listOf(DraftPoint(.2f, .5f), DraftPoint(.8f, .6f)), DraftRedactionMode.FILL, tool, .2f, 0x00FF0000)
            val output = rasterizeImageEdit(bitmap, DraftImageEdit(marks = listOf(mark)))
            try {
                assertEquals(Color.RED, output.getPixel(50, 55))
                assertEquals(Color.WHITE, output.getPixel(5, 5))
            } finally {
                if (output !== bitmap) output.recycle()
                bitmap.recycle()
            }
        }
    }
}
