package com.solthariel.pawchan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ImageReencoding
import com.solthariel.pawchan.composer.imageOrientation
import com.solthariel.pawchan.composer.prepareComposerAttachments
import com.solthariel.pawchan.composer.reencodeAttachmentImage
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class ComposerMediaProcessingTest {
    @Test
    fun pngUniqueHashPreservesPixelsAndNeverMutatesOriginal() =
        runBlocking {
            val original = image(Bitmap.CompressFormat.PNG)
            val saved = original.copyOf()
            val file =
                ComposerAttachment(
                    ReplyAttachment("original.png", "image/png", original),
                    options =
                        AttachmentEditOptions(uniqueHash = true, removeMetadata = true, customName = "renamed"),
                )
            val result = prepareComposerAttachments(listOf(file), PostingRules()) as PostingOutcome.Success
            val uploaded = result.value.single()
            assertArrayEquals(saved, original)
            assertFalse(uploaded.content.contentEquals(original))
            assertEquals("renamed.png", uploaded.fileName)
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(uploaded.content, 0, uploaded.content.size))
            try {
                assertEquals(12, decoded.width)
                assertEquals(20, decoded.height)
                assertEquals(0xff123456.toInt(), decoded.getPixel(0, 0))
            } finally {
                decoded.recycle()
            }
        }

    @Test
    fun reencodingChangesRealBytesDimensionsMimeAndExtensionTogether() =
        runBlocking {
            val original = image(Bitmap.CompressFormat.PNG)
            val file =
                ComposerAttachment(
                    ReplyAttachment("original.png", "image/png", original),
                    options =
                        AttachmentEditOptions(
                            reencoding = ImageReencoding(jpeg = true, quality = 90, scalePercent = 50),
                        ),
                )
            val outcome = prepareComposerAttachments(listOf(file), PostingRules()) as PostingOutcome.Success
            val uploaded = outcome.value.single()
            assertEquals("original.jpg", uploaded.fileName)
            assertEquals("image/jpeg", uploaded.mimeType)
            val decoded = requireNotNull(BitmapFactory.decodeByteArray(uploaded.content, 0, uploaded.content.size))
            try {
                assertEquals(6, decoded.width)
                assertEquals(10, decoded.height)
            } finally {
                decoded.recycle()
            }
        }

    @Test
    fun divisorEightUsesExactOneEighthDimensions() {
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val original =
            try {
                ByteArrayOutputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    output.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        val result = reencodeAttachmentImage(original, ImageReencoding(jpeg = false, scaleDivisor = 8))
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(result, 0, result.size))
        try {
            assertEquals(100, decoded.width)
            assertEquals(50, decoded.height)
        } finally {
            decoded.recycle()
        }
    }

    @Test
    fun removingExifBakesOrientationInsteadOfTurningPhotoSideways() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val temporary = File.createTempFile("posting-exif", ".jpg", context.cacheDir)
            try {
                temporary.writeBytes(image(Bitmap.CompressFormat.JPEG))
                ExifInterface(temporary.path).apply {
                    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                    saveAttributes()
                }
                val original = temporary.readBytes()
                val saved = original.copyOf()
                val file =
                    ComposerAttachment(
                        ReplyAttachment("photo.jpg", "image/jpeg", original),
                        options =
                            AttachmentEditOptions(removeMetadata = true),
                    )
                val outcome = prepareComposerAttachments(listOf(file), PostingRules()) as PostingOutcome.Success
                val uploaded = outcome.value.single()
                assertArrayEquals(saved, original)
                val decoded = requireNotNull(BitmapFactory.decodeByteArray(uploaded.content, 0, uploaded.content.size))
                try {
                    assertEquals(20, decoded.width)
                    assertEquals(12, decoded.height)
                    assertEquals(1, imageOrientation(uploaded.content))
                } finally {
                    decoded.recycle()
                }
            } finally {
                temporary.delete()
            }
        }

    private fun image(format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(12, 20, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(0xff123456.toInt())
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(format, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
