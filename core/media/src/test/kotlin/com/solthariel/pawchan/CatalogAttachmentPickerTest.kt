package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.attachmentTypeAndSize
import com.solthariel.pawchan.media.calculateAttachmentGridMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogAttachmentPickerTest {
    @Test
    fun eightAttachmentsFitFourRowsWithoutScrolling() {
        val metrics =
            calculateAttachmentGridMetrics(
                itemCount = 8,
                availableWidth = 360f,
                availableHeight = 480f,
                maximumCellSize = 120f,
                gap = 8f,
            )

        assertEquals(4, metrics.rows)
        assertEquals(114f, metrics.cellSize, 0f)
    }

    @Test
    fun twoAttachmentsKeepTheMaximumReadableCellSize() {
        val metrics =
            calculateAttachmentGridMetrics(
                itemCount = 2,
                availableWidth = 360f,
                availableHeight = 640f,
                maximumCellSize = 120f,
                gap = 8f,
            )

        assertEquals(1, metrics.rows)
        assertEquals(120f, metrics.cellSize, 0f)
    }

    @Test
    fun attachmentMetadataMatchesDvachGridStyle() {
        val attachment =
            MediaAttachmentUi(
                source =
                    MediaAttachmentSource(
                        id = "image",
                        fileName = "photo.png",
                        originalUrl = "https://2ch.su/photo.png",
                        thumbnailUrl = null,
                    ),
                byteSize = 1_363_149L,
                viewer = AttachmentViewer.IMAGE,
                dimensions = MediaDimensions.create(1024, 1320),
            )

        assertEquals("PNG 1.4 МБ", attachmentTypeAndSize(attachment))
        assertEquals("1024×1320", "${attachment.dimensions?.width}×${attachment.dimensions?.height}")
    }
}
