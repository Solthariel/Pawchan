package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.attachmentFormat
import com.solthariel.pawchan.media.attachmentTypeAndSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MediaCaptionFormattingTest {
    @Test
    fun determinesPngFromOriginalWhenDisplayNameHasNoExtension() {
        val attachment = attachment("image", "17912254050410454222.png", 198L * 1024, 447, 447)

        assertEquals("image", attachment.fileName)
        assertEquals("PNG 202 КБ", attachment.display.typeAndSize)
        assertEquals("447×447", attachment.displayDimensions)
    }

    @Test
    fun originalExtensionWinsOverDisplayNameAndThumbnail() {
        val attachment = attachment("photo.jpeg", "17909215941480535899.png", 183L * 1024, 1440, 1440)

        assertEquals("PNG", attachment.displayFormat)
        assertEquals("1440×1440", attachment.displayDimensions)
    }

    @Test
    fun preservesMmaOriginalDimensions() {
        val attachment = attachment("798021764204155[...].jpeg", "17909215941480535899.jpg", 183L * 1024, 1440, 1440)

        assertEquals("JPEG 187 КБ", attachment.display.typeAndSize)
        assertEquals("1440×1440", attachment.displayDimensions)
        assertFalse(attachment.display.typeAndSize.contains('·'))
        assertFalse(attachment.display.typeAndSize.contains('\n'))
    }

    @Test
    fun normalizesJpgAndJpegToOneLabel() {
        assertEquals("JPEG", attachmentFormat("https://2ch.su/a.jpg", "a.jpeg"))
        assertEquals("JPEG", attachmentFormat("https://2ch.su/a.JPEG", "a.jpg"))
    }

    @Test
    fun stripsQueryAndFragmentBeforeDetectingExtension() {
        assertEquals("WEBP", attachmentFormat("https://2ch.su/a.webp?download=1#preview", "image"))
    }

    @Test
    fun fallsBackToDisplayNameOnlyWhenOriginalHasNoExtension() {
        assertEquals("PNG", attachmentFormat("https://2ch.su/download", "photo.png"))
        assertEquals("ФАЙЛ", attachmentFormat("https://2ch.su/download", "image"))
    }

    @Test
    fun leavesUnknownSizeAndDimensionsAbsent() {
        val attachment = attachment("image", "original.png", null, 0, 0)

        assertEquals("PNG", attachment.display.typeAndSize)
        assertNull(attachment.displayDimensions)
    }

    @Test
    fun usesOneSizeFormatterForCaptionAndComposer() {
        val attachment = attachment("image", "original.png", 2_202_010, 811, 1000)

        assertEquals("PNG ${formatByteSizeForUi(2_202_010)}", attachmentTypeAndSize(attachment))
    }

    private fun attachment(
        name: String,
        originalName: String,
        bytes: Long?,
        width: Int,
        height: Int,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            source =
                MediaAttachmentSource(
                    id = originalName,
                    fileName = name,
                    originalUrl = "https://2ch.su/sp/src/3352871/$originalName",
                    thumbnailUrl = "https://2ch.su/sp/thumb/3352871/thumb.jpg",
                ),
            byteSize = bytes,
            viewer = AttachmentViewer.IMAGE,
            dimensions = MediaDimensions.create(width, height),
        )
}
