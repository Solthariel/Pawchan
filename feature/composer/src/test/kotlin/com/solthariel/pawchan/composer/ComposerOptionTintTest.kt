package com.solthariel.pawchan.composer

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ComposerOptionTintTest {
    @Test fun selectedAndUnselectedFollowNavigationRolesInLightDarkAndColoredThemes() {
        val schemes =
            listOf(
                lightColorScheme(primary = Color.Black, onSurfaceVariant = Color.DarkGray),
                darkColorScheme(primary = Color.White, onSurfaceVariant = Color.LightGray),
                lightColorScheme(primary = Color(0xFF6650A4), onSurfaceVariant = Color(0xFF49454F)),
                darkColorScheme(primary = Color(0xFFD0BCFF), onSurfaceVariant = Color(0xFFCAC4D0)),
            )
        schemes.forEach { colors ->
            val selected = composerOptionTint(colors, checked = true, enabled = true)
            val unselected = composerOptionTint(colors, checked = false, enabled = true)
            val disabled = composerOptionTint(colors, checked = false, enabled = false)
            assertEquals(colors.primary, selected)
            assertEquals(colors.onSurfaceVariant.copy(alpha = 0.55f), unselected)
            assertEquals(colors.onSurface.copy(alpha = 0.38f), disabled)
            assertNotEquals(selected, unselected)
            assertNotEquals(unselected, disabled)
        }
    }
}
