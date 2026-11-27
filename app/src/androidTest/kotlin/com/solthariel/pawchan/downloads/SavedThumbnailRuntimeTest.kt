package com.solthariel.pawchan.downloads

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SavedThumbnailRuntimeTest {
    @Test fun readsFrameFromSavedMovieWithOffsetWithoutDependingOnAProviderThumbnail() =
        runBlocking {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val dir = File(context.cacheDir, "media_clipboard").apply { mkdirs() }
            val file = File.createTempFile("saved-offset-", ".mp4", dir)
            try {
                instrumentation.context.assets.open("downloads/offset-blue.mp4").use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
                val bitmap = checkNotNull(savedDocumentThumbnail(context.contentResolver, uri, file.name, 64))
                try {
                    assertTrue(bitmap.width in 1..64 && bitmap.height in 1..64)
                    val color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
                    assertTrue("Expected blue source frame, not a placeholder", Color.blue(color) > Color.red(color) + 60)
                } finally {
                    bitmap.recycle()
                }
                file.writeBytes(byteArrayOf(5, 6, 7))
                assertNull(savedDocumentThumbnail(context.contentResolver, uri, file.name, 64))
            } finally {
                file.delete()
            }
        }

    @Test fun savedImageIsDecodedSmallAndMissingFileKeepsPlaceholder() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val dir = File(context.cacheDir, "media_clipboard").apply { mkdirs() }
            val file = File.createTempFile("saved-image-", ".jpg", dir)
            try {
                val source = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888)
                try {
                    source.eraseColor(Color.GREEN)
                    file.outputStream().use { output -> check(source.compress(Bitmap.CompressFormat.JPEG, 90, output)) }
                } finally {
                    source.recycle()
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.media", file)
                val result = checkNotNull(savedDocumentThumbnail(context.contentResolver, uri, file.name, 64))
                try {
                    assertTrue(result.width in 1..64 && result.height in 1..64)
                } finally {
                    result.recycle()
                }
                assertTrue(file.delete())
                assertNull(savedDocumentThumbnail(context.contentResolver, uri, file.name, 64))
            } finally {
                file.delete()
            }
        }
}
