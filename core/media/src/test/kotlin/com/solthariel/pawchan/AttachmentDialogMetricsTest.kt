package com.solthariel.pawchan

import com.solthariel.pawchan.media.attachmentDialogMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentDialogMetricsTest {
    @Test fun wideImageShrinksWindowInsteadOfKeepingBlankMediaSpace() {
        val result = attachmentDialogMetrics(740, 70, 304, 8, 360, 2f)
        assertEquals(180, result.mediaHeight)
        assertEquals(570, result.totalHeight)
    }

    @Test fun tallImageIsBoundedByTheControlsAndScreen() {
        val result = attachmentDialogMetrics(740, 70, 304, 8, 360, 0.5f)
        assertEquals(350, result.mediaHeight)
        assertEquals(740, result.totalHeight)
    }

    @Test fun extremeHeightSacrificesMediaBeforeMakingControlsInaccessible() {
        val result = attachmentDialogMetrics(200, 70, 304, 8, 360, 2f)
        assertEquals(0, result.mediaHeight)
        assertEquals(114, result.footerHeight)
        assertTrue(result.totalHeight <= 200)
    }
}
