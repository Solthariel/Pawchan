package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import com.solthariel.pawchan.ui.appTypography
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TypographySettingsRuntimeTest(
    private val font: AppFont,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settingsRemainVisibleAndClickableOnNarrowScreen() {
        val checked = mutableStateOf(true)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme(typography = appTypography(TypographyPreferences(font = font))) {
                    Box(Modifier.width(320.dp)) {
                        Column {
                            SettingsItemRow(
                                SettingsItem.Toggle(
                                    "Восстанавливать последний экран",
                                    "При запуске",
                                    checked.value,
                                ) { checked.value = it },
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("При запуске").assertIsDisplayed()
        compose
            .onNodeWithText("Восстанавливать последний экран")
            .assertHeightIsAtLeast(48.dp)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, scale={1}")
        fun cases(): List<Array<Any>> = AppFont.entries.flatMap { font -> listOf(1f, 1.6f).map { arrayOf(font, it) } }
    }
}
