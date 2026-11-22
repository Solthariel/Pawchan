package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EditedAttachmentPreparationTest {
    private val source = byteArrayOf(-1, -40, -1, -39)
    private val edit = DraftImageEdit(quarterTurns = 1)

    @Test
    fun stripsMetadataAndAddsUniqueHashWithoutChangingOriginal() {
        val file = attachment()
        val rendered = byteArrayOf(-1, -40, -1, -31, 0, 4, 42, 43, -1, -39)
        val result =
            prepareEditedAttachment(file) { _, recipe, _ ->
                assertEquals(edit, recipe)
                ImageEditOutput(rendered, "jpg", "image/jpeg")
            }
        assertEquals("photo.jpg", result.fileName)
        assertEquals("image/jpeg", result.mimeType)
        assertTrue(result.spoiler)
        assertArrayEquals(byteArrayOf(-1, -40, -1, -39), source)
        assertFalse(source.contentEquals(result.content))
        assertFalse(
            result.content
                .asList()
                .windowed(2)
                .any { it == listOf(42.toByte(), 43.toByte()) },
        )
    }

    @Test
    fun rejectsWrongFormatAndFailedExportWithoutFallingBack() {
        val file = attachment()
        assertThrows(IllegalArgumentException::class.java) {
            prepareEditedAttachment(file) { _, _, _ -> ImageEditOutput(source, "png", "image/png") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepareEditedAttachment(file) { _, _, _ -> ImageEditOutput(byteArrayOf(1), "jpg", "image/jpeg") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            prepareEditedAttachment(file) { _, _, _ -> throw IllegalArgumentException("decode") }
        }
    }

    private fun attachment() =
        ComposerAttachment(
            source = ReplyAttachment("photo.jpg", "image/jpeg", source),
            options = MediaDraftOptions(image = DraftImageOptions(edit = edit), spoiler = true),
        )
}
