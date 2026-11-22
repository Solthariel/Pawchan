package com.solthariel.pawchan

import com.solthariel.pawchan.composer.AttachmentEditOptions
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ImageReencoding
import com.solthariel.pawchan.composer.attachmentUploadName
import com.solthariel.pawchan.composer.validAttachmentName
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ComposerAttachmentNamesTest {
    private val file = ComposerAttachment(ReplyAttachment("cat.png", "image/png", byteArrayOf(1)))

    @Test
    fun renameAndReencodingKeepTheRealOutputExtension() {
        assertEquals(
            "new.jpg",
            attachmentUploadName(
                file.copy(
                    options =
                        AttachmentEditOptions(
                            customName = "new",
                            reencoding = ImageReencoding(),
                        ),
                ),
            ),
        )
        assertEquals(
            "file.png",
            attachmentUploadName(
                file.copy(
                    options =
                        AttachmentEditOptions(
                            customName = "ignored",
                            removeFileName = true,
                        ),
                ),
            ),
        )
        assertEquals("cat.png", attachmentUploadName(file))
    }

    @Test
    fun editedWebpUsesPngExtensionUnlessJpegIsSelected() {
        val webp = "RIFF0000WEBP".encodeToByteArray()
        val edited =
            ComposerAttachment(
                ReplyAttachment("cat.webp", "image/webp", webp),
                options = AttachmentEditOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1))),
            )
        assertEquals("cat.png", attachmentUploadName(edited))
        val jpeg = edited.copy(options = edited.options.copy(reencoding = ImageReencoding(jpeg = true)))
        assertEquals("cat.jpg", attachmentUploadName(jpeg))
        assertEquals("file.jpg", attachmentUploadName(jpeg.copy(options = jpeg.options.copy(removeFileName = true))))
    }

    @Test
    fun editedJpegKeepsJpegExtensionEvenIfNameHadDifferentSuffix() {
        val jpeg = byteArrayOf(-1, -40, -1)
        val file =
            ComposerAttachment(
                ReplyAttachment("photo.bin", "image/jpeg", jpeg),
                options = AttachmentEditOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1))),
            )
        assertEquals("photo.jpg", attachmentUploadName(file))
    }

    @Test
    fun invalidRenameCannotBeSubmitted() {
        assertFalse(file.copy(options = AttachmentEditOptions(customName = "")).validName)
        assertFalse(validAttachmentName("../name"))
        assertFalse(validAttachmentName("name\r\nheader"))
        assertFalse(validAttachmentName("folder\\name"))
    }
}
