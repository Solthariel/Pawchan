package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.SuccessResult
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.composerThumbnailRequest
import com.solthariel.pawchan.composer.prepareComposerThumbnails
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ComposerThumbnailPreparationRuntimeTest {
    @Test fun firstCellRequestUsesTheAlreadyDecodedImportPreview() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val loader = ImageLoader.Builder(context).build()
            try {
                val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                val bytes =
                    ByteArrayOutputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        it.toByteArray()
                    }
                bitmap.recycle()
                val file = ComposerAttachment(ReplyAttachment("test.png", "image/png", bytes))
                prepareComposerThumbnails(context, listOf(file), loader)
                val firstCell = loader.execute(composerThumbnailRequest(context, file))
                assertTrue(firstCell is SuccessResult)
                assertEquals(DataSource.MEMORY_CACHE, (firstCell as SuccessResult).dataSource)
                assertTrue(firstCell.image.width <= 384 && firstCell.image.height <= 384)
            } finally {
                loader.shutdown()
            }
        }
}
