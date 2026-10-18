package com.solthariel.pawchan

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.infrastructure.thread.export.AppThreadExportTarget
import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportPost
import com.solthariel.pawchan.thread.ThreadShotStyle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class ThreadExportRuntimeTest {
    @Test fun htmlTargetWritesUtf8AndEscapedTextToChosenFile() =
        runBlocking {
            withTarget { target, file ->
                target.saveHtml(ThreadExportDocument(12, "Тема", listOf(post())))
                val html = file.readText(Charsets.UTF_8)
                assertTrue(html.contains("Тема"))
                assertTrue(html.contains("Текст &lt;script&gt; &amp;"))
                assertFalse(html.contains("<script>"))
            }
        }

    @Test fun pngTargetUsesScreenWidthAndCapturedBackground() =
        runBlocking {
            withTarget { target, file ->
                val style = ThreadShotStyle(background = 0xFF112233.toInt())
                target.saveShot(listOf(post()), style)
                val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path))
                try {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    assertEquals(context.resources.configuration.screenWidthDp * 2, bitmap.width)
                    assertTrue(bitmap.height > 0)
                    assertEquals(style.background, bitmap.getPixel(0, 0))
                } finally {
                    bitmap.recycle()
                }
            }
        }

    @Test fun emptySelectionFailsWithoutProducingPng() =
        runBlocking {
            withTarget { target, file ->
                var error: IOException? = null
                try {
                    target.saveShot(emptyList(), ThreadShotStyle())
                } catch (failure: IOException) {
                    error = failure
                }
                assertEquals("Слишком большой скриншот: выберите меньше постов", error?.message)
                assertEquals(0L, file.length())
            }
        }

    @Test fun pngTargetPreservesTopAndBottomMargins() =
        runBlocking {
            withTarget { target, file ->
                val style =
                    ThreadShotStyle(
                        background = 0xFF112233.toInt(),
                        surface = 0xFF445566.toInt(),
                        border = 0xFFFF0000.toInt(),
                    )
                for (posts in listOf(listOf(post()), listOf(post(), post()))) {
                    target.saveShot(posts, style)
                    val bitmap = requireNotNull(BitmapFactory.decodeFile(file.path))
                    try {
                        val centerX = bitmap.width / 2
                        for (y in 0 until 15) {
                            for (x in 0 until bitmap.width) {
                                assertEquals(style.background, bitmap.getPixel(x, y))
                                assertEquals(style.background, bitmap.getPixel(x, bitmap.height - 1 - y))
                            }
                        }
                        assertEquals(style.border, bitmap.getPixel(centerX, 16))
                        assertEquals(style.border, bitmap.getPixel(centerX, bitmap.height - 16))
                        assertEquals(style.surface, bitmap.getPixel(centerX, 18))
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }

    private fun post(): ThreadExportPost =
        ThreadExportPost(1, AnnotatedString("#12 Автор"), null, PostDocument.plain("Текст <script> &"), emptyList())

    private suspend fun withTarget(block: suspend (AppThreadExportTarget, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ImageLoader.Builder(context).build()
        val file = File.createTempFile("thread-export-", ".tmp", context.cacheDir)
        try {
            block(AppThreadExportTarget(context, loader, Uri.fromFile(file)), file)
        } finally {
            loader.shutdown()
            file.delete()
        }
    }
}
