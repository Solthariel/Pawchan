package com.solthariel.pawchan

import com.solthariel.pawchan.domain.settings.DownloadNaming
import com.solthariel.pawchan.infrastructure.media.downloadName
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNamingTest {
    private fun attachment(name: String) =
        MediaAttachmentUi(
            MediaAttachmentSource("1", name, "https://example.com/image.jpg", null, 45L, "dvach_b_42_45"),
            null,
            AttachmentViewer.IMAGE,
        )

    @Test fun originalAndDetailedNamesAreDistinctFromUploadOptions() {
        assertEquals("photo.jpg", downloadName(attachment("photo.jpg"), DownloadNaming.ORIGINAL))
        assertEquals("dvach_b_42_45_photo.jpg", downloadName(attachment("photo.jpg"), DownloadNaming.DETAILED))
    }

    @Test fun namesCannotEscapeDirectoryAndKeepExtensionWhenTruncated() {
        assertEquals("photo.jpg", downloadName(attachment("../../photo.jpg"), DownloadNaming.ORIGINAL))
        val name = downloadName(attachment("a".repeat(300) + ".png"), DownloadNaming.DETAILED)
        assertTrue(name.length <= 180)
        assertTrue(name.endsWith(".png"))
    }
}
