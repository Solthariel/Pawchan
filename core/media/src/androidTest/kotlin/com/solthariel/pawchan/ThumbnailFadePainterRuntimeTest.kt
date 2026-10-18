package com.solthariel.pawchan

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.solthariel.pawchan.media.thumbnail.ThumbnailFadePainter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailFadePainterRuntimeTest {
    @Test
    fun drawReadsTheCurrentAlphaWithoutAComposition() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        val opacity = mutableFloatStateOf(0f)
        val painter = ThumbnailFadePainter(BitmapPainter(source.asImageBitmap()), opacity)
        val scope = CanvasDrawScope()
        val canvas = Canvas(output.asImageBitmap())
        try {
            for (value in listOf(0f, 0.5f, 1f)) {
                opacity.floatValue = value
                output.eraseColor(Color.TRANSPARENT)
                scope.draw(Density(1f), LayoutDirection.Ltr, canvas, Size(8f, 8f)) {
                    with(painter) { draw(size) }
                }
                val alpha = Color.alpha(output.getPixel(4, 4))
                if (value == 0.5f) assertTrue(alpha in 127..128) else assertEquals((255 * value).toInt(), alpha)
            }
        } finally {
            source.recycle()
            output.recycle()
        }
    }
}
