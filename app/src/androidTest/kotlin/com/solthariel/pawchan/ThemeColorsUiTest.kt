package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsGroupCard
import com.solthariel.pawchan.settings.SettingsUiState
import com.solthariel.pawchan.settings.availableThemePalettes
import com.solthariel.pawchan.settings.themeSettingsGroup
import com.solthariel.pawchan.ui.PawchanTheme
import com.solthariel.pawchan.ui.presetColorScheme
import com.solthariel.pawchan.ui.withPureBlackSurfaces
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeColorsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun paletteSelectionUpdatesThemeWithoutChangingMode() {
        val state = mutableStateOf(SettingsUiState(theme = ThemePreference.DARK))
        var rendered: ColorScheme? = null
        compose.setContent {
            PawchanTheme(state.value.theme, state.value.themeColors) {
                rendered = MaterialTheme.colorScheme
                Column {
                    SettingsGroupCard(
                        themeSettingsGroup(
                            state.value,
                            SettingsActions(
                                { state.value = state.value.copy(theme = it) },
                                {},
                                {},
                                onThemeColors = { state.value = state.value.copy(themeColors = it) },
                            ),
                        ),
                    )
                }
            }
        }
        compose.onNodeWithText("Цветовая палитра").performClick()
        compose.onNodeWithText("Васильковая").performClick()
        compose.runOnIdle {
            assertEquals(ThemePalette.BLUE, state.value.themeColors.palette)
            assertEquals(ThemePreference.DARK, state.value.theme)
            assertEquals(presetColorScheme(ThemePalette.BLUE, true).primary, rendered?.primary)
        }
        compose.onNodeWithText("Васильковая").assertIsDisplayed()
    }

    @Test fun amoledModeKeepsChosenPalette() {
        val state = mutableStateOf(SettingsUiState(themeColors = ThemeColors(ThemePalette.BURGUNDY)))
        compose.setContent {
            PawchanTheme(state.value.theme, state.value.themeColors) {
                SettingsGroupCard(
                    themeSettingsGroup(
                        state.value,
                        SettingsActions(
                            { state.value = state.value.copy(theme = it) },
                            {},
                            {},
                            onThemeColors = { state.value = state.value.copy(themeColors = it) },
                        ),
                    ),
                )
            }
        }
        compose.onNodeWithText("Режим темы").performClick()
        compose.onNodeWithText("AMOLED").performClick()
        compose.runOnIdle {
            assertEquals(ThemePreference.AMOLED, state.value.theme)
            assertEquals(ThemeColors(ThemePalette.BURGUNDY), state.value.themeColors)
        }
        compose.onNodeWithText("Tomorrow").assertDoesNotExist()
        compose.onNodeWithText("Burichan").assertDoesNotExist()
    }

    @Test fun blackSurfacesDoNotReplaceTheAccent() {
        ThemePalette.entries.forEach { palette ->
            val original = presetColorScheme(palette, true)
            val black = original.withPureBlackSurfaces()
            assertEquals(Color.Black, black.background)
            assertEquals(Color.Black, black.surfaceContainer)
            assertEquals(original.primary, black.primary)
            assertEquals(original.primaryContainer, black.primaryContainer)
            assertEquals(original.onSurface, black.onSurface)
        }
    }

    @Test fun blackOverrideDoesNotAffectLightMode() {
        var background: Color? = null
        compose.setContent {
            PawchanTheme(ThemePreference.LIGHT, ThemeColors(ThemePalette.OLIVE, true)) {
                background = MaterialTheme.colorScheme.background
            }
        }
        compose.runOnIdle { assertEquals(presetColorScheme(ThemePalette.OLIVE, false).background, background) }
    }

    @Test fun wallpaperChoiceIsOnlyOfferedOnSupportedAndroid() {
        assertFalse(ThemePalette.SYSTEM in availableThemePalettes(30))
        assertTrue(ThemePalette.SYSTEM in availableThemePalettes(31))
        assertEquals(
            presetColorScheme(ThemePalette.LAVENDER, false).primary,
            presetColorScheme(ThemePalette.SYSTEM, false).primary,
        )
    }

    @Test fun wallpaperSchemeUsesSystemResourcesOrSafeFallback() {
        var actual: Color? = null
        var expected: Color? = null
        compose.setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            expected =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    androidx.compose.material3
                        .dynamicLightColorScheme(context)
                        .primary
                } else {
                    presetColorScheme(ThemePalette.LAVENDER, false).primary
                }
            PawchanTheme(ThemePreference.LIGHT, ThemeColors(ThemePalette.SYSTEM)) {
                actual = MaterialTheme.colorScheme.primary
            }
        }
        compose.runOnIdle { assertEquals(expected, actual) }
    }
}
