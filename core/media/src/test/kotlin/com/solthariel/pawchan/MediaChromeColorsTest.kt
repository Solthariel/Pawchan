package com.solthariel.pawchan

import androidx.compose.ui.graphics.toArgb
import com.solthariel.pawchan.media.MEDIA_CHROME_BACKGROUND
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaChromeColorsTest {
    @Test
    fun overlayUsesExactMediaOverlayColorNotTheAppTheme() {
        assertEquals(0xAA202020.toInt(), MEDIA_CHROME_BACKGROUND.toArgb())
        assertEquals(0xAA / 255f, MEDIA_CHROME_BACKGROUND.alpha, 0.0001f)
    }
}
