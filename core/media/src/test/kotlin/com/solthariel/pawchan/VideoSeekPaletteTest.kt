package com.solthariel.pawchan

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.solthariel.pawchan.media.video.videoSeekActiveColor
import com.solthariel.pawchan.media.video.videoSeekInactiveColor
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSeekPaletteTest {
    @Test
    fun seekUsesTheSameRolesAsPostingOptions() {
        val schemes =
            listOf(
                lightColorScheme(primary = Color.Red, onSurfaceVariant = Color.Blue, onSurface = Color.Black),
                darkColorScheme(primary = Color.Green, onSurfaceVariant = Color.Cyan, onSurface = Color.White),
            )
        for (colors in schemes) {
            assertEquals(colors.primary, videoSeekActiveColor(colors, true))
            assertEquals(colors.onSurfaceVariant.copy(alpha = 0.55f), videoSeekInactiveColor(colors, true))
            assertEquals(colors.onSurface.copy(alpha = 0.38f), videoSeekActiveColor(colors, false))
            assertEquals(colors.onSurface.copy(alpha = 0.38f), videoSeekInactiveColor(colors, false))
        }
    }
}
