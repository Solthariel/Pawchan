package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsGroupCard
import com.solthariel.pawchan.settings.SettingsUiState
import com.solthariel.pawchan.settings.themeSettingsGroup
import com.solthariel.pawchan.ui.PawchanTheme
import com.solthariel.pawchan.ui.generatedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomColorUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(SettingsUiState(theme = ThemePreference.AMOLED))
    private var background: Color? = null
    private var accent: Color? = null

    @Test fun lavenderPrefillsRgbWithoutChangingSelectedPalette() {
        val lavender = ThemeColors(palette = ThemePalette.LAVENDER)
        state.value = state.value.copy(themeColors = lavender)
        showSettings()
        openCustom()
        assertSeed("#6750A4", 103f, 80f, 164f)
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(lavender, state.value.themeColors) }
    }

    @Test fun peachPrefillsRgbInsteadOfThePreviouslySavedCustomSeed() {
        state.value = state.value.copy(themeColors = ThemeColors(customSeed = CUSTOM_RGB))
        showSettings()
        compose.onNodeWithText("Цветовая палитра").performClick()
        compose.onNodeWithText("Персиковая").performScrollTo().performClick()
        openCustom()
        assertSeed("#B85C26", 184f, 92f, 38f)
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle {
            assertEquals(ThemePalette.PEACH, state.value.themeColors.palette)
            assertEquals(CUSTOM_RGB, state.value.themeColors.customSeed)
        }
    }

    @Test fun savedCustomSeedReopensWithoutBeingReplacedByPreset() {
        state.value =
            state.value.copy(themeColors = ThemeColors(palette = ThemePalette.CUSTOM, customSeed = CUSTOM_RGB))
        showSettings()
        openCustom()
        assertSeed("#23A080", 35f, 160f, 128f)
    }

    @Test fun applyCustomColorChangesAccentAndKeepsAmoled() {
        showSettings()
        openCustom()
        compose.onNodeWithText("Цвет HEX").performTextReplacement("#23A080")
        awaitPreview()
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertEquals(ThemePalette.CUSTOM, state.value.themeColors.palette)
            assertEquals(CUSTOM_RGB, state.value.themeColors.customSeed)
            assertEquals(ThemePreference.AMOLED, state.value.theme)
            assertEquals(Color.Black, background)
            assertEquals(generatedColorScheme(CUSTOM_RGB, true).primary, accent)
        }
    }

    @Test fun cancelDoesNotSaveDraftOrChangeActualAppTheme() {
        showSettings()
        openCustom()
        compose.onNodeWithText("Цвет HEX").performTextReplacement("#23A080")
        awaitPreview()
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(ThemeColors(), state.value.themeColors) }
    }

    @Test fun incompleteHexCannotBeApplied() {
        showSettings()
        openCustom()
        compose.onNodeWithText("Цвет HEX").performTextReplacement("#12")
        compose.onNodeWithText("Применить").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(ThemeColors(), state.value.themeColors) }
    }

    @Test fun previewStaysVisibleWhenHexBecomesIncomplete() {
        showSettings()
        openCustom()
        awaitPreview()
        compose.onNodeWithText("Цвет HEX").performTextReplacement("#12")
        compose.onNodeWithText("Предпросмотр").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Применить").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(ThemeColors(), state.value.themeColors) }
    }

    @Test fun finalPresetCanBeReachedByScrollingAndSelected() {
        showSettings()
        compose.onNodeWithText("Цветовая палитра").performClick()
        compose.onNodeWithText("Галечная").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(ThemePalette.GRAY, state.value.themeColors.palette)
            assertEquals(Color.Black, background)
        }
    }

    @Test fun draftSurvivesStateRestorationWithoutSavingSettings() {
        val restoration = StateRestorationTester(compose)
        showSettings(restoration)
        openCustom()
        compose.onNodeWithText("Цвет HEX").performTextReplacement("#23A080")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Цвет HEX").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("#23A080")),
        )
        compose.runOnIdle { assertEquals(ThemeColors(), state.value.themeColors) }
    }

    private fun assertSeed(
        hex: String,
        red: Float,
        green: Float,
        blue: Float,
    ) {
        compose.onNodeWithText("Цвет HEX").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(hex)),
        )
        listOf("Красный" to red, "Зелёный" to green, "Синий" to blue).forEach { (label, expected) ->
            val node = compose.onNodeWithContentDescription(label).performScrollTo().fetchSemanticsNode()
            assertEquals(expected, node.config[SemanticsProperties.ProgressBarRangeInfo].current, 0f)
        }
    }

    private fun showSettings(restoration: StateRestorationTester? = null) {
        val content: @Composable () -> Unit = {
            PawchanTheme(state.value.theme, state.value.themeColors) {
                background = MaterialTheme.colorScheme.background
                accent = MaterialTheme.colorScheme.primary
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
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
    }

    private fun openCustom() {
        compose.onNodeWithText("Цветовая палитра").performClick()
        compose.onNodeWithText("Свой цвет").performScrollTo().performClick()
    }

    private fun awaitPreview() {
        compose.waitUntil(timeoutMillis = PREVIEW_TIMEOUT_MS) {
            runCatching { compose.onNodeWithText("Применить").assertIsEnabled() }.isSuccess
        }
    }
}

private const val CUSTOM_RGB = 0x23A080
private const val PREVIEW_TIMEOUT_MS = 5_000L
