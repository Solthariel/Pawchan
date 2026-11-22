package com.solthariel.pawchan.ui

import androidx.compose.material3.Typography
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.FontEmphasis
import com.solthariel.pawchan.domain.settings.TextLeading
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTypographyTest {
    @Test
    fun defaultPreservesAllMaterialStyles() {
        assertEquals(Typography(), buildAppTypography(TypographyPreferences()))
    }

    @Test
    fun supportedFontsRetainWeightAndSizeDifferences() {
        val original = Typography()
        for (font in AppFont.entries.filter { it != AppFont.SYSTEM }) {
            val custom = buildAppTypography(TypographyPreferences(font = font))
            assertNotNull(custom.bodyMedium.fontFamily)
            assertEquals(original.bodyMedium.fontSize, custom.bodyMedium.fontSize)
            assertTrue(custom.titleMedium.fontWeight!!.weight > custom.bodyMedium.fontWeight!!.weight)
        }
    }

    @Test
    fun emphasisAndLeadingChangeWithoutChangingFontSize() {
        val original = Typography().bodyMedium
        val custom =
            buildAppTypography(
                TypographyPreferences(emphasis = FontEmphasis.STRONG, leading = TextLeading.RELAXED),
            ).bodyMedium
        assertEquals(original.fontSize, custom.fontSize)
        assertTrue(custom.fontWeight!!.weight > original.fontWeight!!.weight)
        assertTrue(custom.lineHeight.value > original.lineHeight.value)
    }
}
