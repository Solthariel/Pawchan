package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayOutputStream

class EditedAttachmentRuntimeTest {
    @Test
    fun redactionIsBakedIntoPreparedUploadAndOriginalIsUnchanged() {
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val source =
            try {
                ByteArrayOutputStream().use { stream ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
                    stream.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        val original = source.copyOf()
        val redaction =
            DraftRedaction(
                listOf(DraftPoint(.25f, .25f), DraftPoint(.75f, .75f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val file =
            ComposerAttachment(
                source = ReplyAttachment("private.png", "image/png", source),
                options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(marks = listOf(redaction)))),
            )
        val upload = prepareEditedAttachment(file, ::exportImageEdit)
        assertEquals("private.png", upload.fileName)
        assertEquals("image/png", upload.mimeType)
        assertFalse(upload.content.contentEquals(original))
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(upload.content, 0, upload.content.size))
        try {
            assertEquals(Color.BLACK, decoded.getPixel(10, 10))
            assertEquals(Color.RED, decoded.getPixel(0, 0))
        } finally {
            decoded.recycle()
        }
        assertArrayEquals(original, source)
    }
}
