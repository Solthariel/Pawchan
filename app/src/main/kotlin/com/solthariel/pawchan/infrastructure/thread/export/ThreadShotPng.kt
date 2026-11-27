package com.solthariel.pawchan.infrastructure.thread.export

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.solthariel.pawchan.thread.CapturedThreadShot
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/** Pure pixel transfer: no measuring, scaling, layout, redraw of glyphs, or access to an image loader. */
internal suspend fun CapturedThreadShot.writePng(output: OutputStream) {
    if (strips.size == 1) {
        val strip = strips.single()
        val source = strip.readablePixels() ?: throw IOException("Не удалось прочитать изображение поста")
        try {
            coroutineContext.ensureActive()
            source.encode(output)
        } finally {
            if (source !== strip) source.recycle()
        }
        return
    }
    val result = createBitmap(width, height)
    try {
        val pixels = IntArray(width * minOf(height, COPY_LINES))
        var top = 0
        for (strip in strips) {
            coroutineContext.ensureActive()
            val source = strip.readablePixels()
            if (source == null) throw IOException("Не удалось прочитать изображение поста")
            try {
                var y = 0
                while (y < source.height) {
                    coroutineContext.ensureActive()
                    val lines = minOf(COPY_LINES, source.height - y)
                    source.getPixels(pixels, 0, width, 0, y, width, lines)
                    result.setPixels(pixels, 0, width, 0, top + y, width, lines)
                    y += lines
                }
                top += source.height
            } finally {
                if (source !== strip) source.recycle()
            }
        }
        coroutineContext.ensureActive()
        result.encode(output)
    } finally {
        result.recycle()
    }
}

private const val COPY_LINES = 32

private fun Bitmap.readablePixels(): Bitmap? {
    val hardware = config == Bitmap.Config.HARDWARE
    return if (hardware) copy(Bitmap.Config.ARGB_8888, false) else this
}

private const val PNG_QUALITY = 100

private fun Bitmap.encode(output: OutputStream) {
    val encoded = compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
    if (!encoded) throw IOException("Не удалось записать PNG")
    output.flush()
}
