package com.solthariel.pawchan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.infrastructure.thread.export.AppThreadExportTarget
import com.solthariel.pawchan.thread.CapturedThreadShot
import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportPost
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class ThreadExportRuntimeTest {
    @Test fun htmlTargetWritesUtf8AndEscapedTextToChosenFile() =
        runBlocking {
            withTarget { target, file ->
                val post = ThreadExportPost(1, AnnotatedString("#12 Автор"), null, PostDocument.plain("Текст <script> &"), emptyList())
                target.saveHtml(ThreadExportDocument(12, "Тема", listOf(post)))
                assertTrue(file.readText().contains("Текст &lt;script&gt; &"))
                assertFalse(file.readText().contains("<script>"))
            }
        }

    @Test fun oneOddSizedStripIsNotFramedResampledOrRepainted() = checkTransfer(listOf(23))

    @Test fun rowsAreJoinedAtTheirExactBoundariesAndRetainEveryPixel() = checkTransfer(listOf(11, 48, 7))

    @Test fun emptyAndMixedWidthInputCannotWriteAPartialPng() =
        runBlocking {
            withTarget { _, file ->
                assertThrows(IOException::class.java) { CapturedThreadShot(emptyList()) }
                val first = Bitmap.createBitmap(13, 2, Bitmap.Config.ARGB_8888)
                val second = Bitmap.createBitmap(14, 2, Bitmap.Config.ARGB_8888)
                try {
                    assertThrows(IOException::class.java) { CapturedThreadShot(listOf(first, second)) }
                    assertEquals(0L, file.length())
                } finally {
                    first.recycle()
                    second.recycle()
                }
            }
        }

    private fun checkTransfer(heights: List<Int>) =
        runBlocking {
            withTarget { target, file ->
                val width = 37
                val expected = IntArray(width * heights.sum()) { (0xFF000000.toInt() or ((it * 7867) and 0xFFFFFF)) }
                var row = 0
                val strips =
                    heights.map { height ->
                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                            it.setPixels(expected, row * width, width, 0, 0, width, height)
                            row += height
                        }
                    }
                try {
                    target.saveShot(CapturedThreadShot(strips))
                    val actual = checkNotNull(BitmapFactory.decodeFile(file.path))
                    try {
                        assertEquals(width, actual.width)
                        assertEquals(heights.sum(), actual.height)
                        val pixels = IntArray(expected.size)
                        actual.getPixels(pixels, 0, width, 0, 0, width, actual.height)
                        assertArrayEquals(expected, pixels)
                    } finally {
                        actual.recycle()
                    }
                } finally {
                    strips.forEach(Bitmap::recycle)
                }
            }
        }

    private suspend fun withTarget(block: suspend (AppThreadExportTarget, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("thread-export-", ".tmp", context.cacheDir)
        try {
            block(AppThreadExportTarget(context, Uri.fromFile(file)), file)
        } finally {
            file.delete()
        }
    }
}
