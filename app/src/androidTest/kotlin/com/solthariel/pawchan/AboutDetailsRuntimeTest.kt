package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import com.solthariel.pawchan.settings.AboutLicenses
import com.solthariel.pawchan.settings.AboutLink
import com.solthariel.pawchan.settings.AboutVersions
import com.solthariel.pawchan.settings.RELEASE_URL
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AboutDetailsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun linksOpenTheirOwnDestinations() {
        val opened = mutableListOf<String>()
        val uri =
            object : UriHandler {
                override fun openUri(uri: String) {
                    opened.add(uri)
                }
            }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                MaterialTheme {
                    Column {
                        AboutLink("Релизы на GitHub", "Последний релиз", RELEASE_URL)
                        AboutLink("Сайт автора", "solthariel.github.io", AUTHOR_URL_SAMPLE)
                    }
                }
            }
        }
        compose.onNodeWithText("Релизы на GitHub").performClick()
        compose.onNodeWithText("Сайт автора").performClick()
        compose.runOnIdle { assertEquals(listOf(RELEASE_URL, AUTHOR_URL_SAMPLE), opened) }
    }

    @Test fun componentVersionsAndLicenseMenuOpen() {
        compose.setContent {
            MaterialTheme {
                Column {
                    AboutVersions()
                    AboutLicenses()
                }
            }
        }
        compose.onNodeWithText("Версии компонентов").performClick()
        compose.onNodeWithText(BuildConfig.COMPONENT_VERSIONS).assertIsDisplayed()
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Лицензии").performClick()
        compose.onNodeWithText("Лицензии FOSS").assertIsDisplayed()
        compose.onNodeWithText("Лицензии приложения", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Pawchan").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("FFmpeg").assertIsDisplayed()
    }

    @Test fun licensesShowFontAttributionsInlineAndOpenSourceLinksDirectly() {
        val opened = mutableListOf<String>()
        val uri =
            object : UriHandler {
                override fun openUri(uri: String) {
                    opened.add(uri)
                }
            }
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uri) {
                MaterialTheme { AboutLicenses() }
            }
        }

        compose.onNodeWithText("Лицензии").performClick()
        compose.onNodeWithText("Pawchan").assertIsDisplayed().assertHasNoClickAction()
        compose.onNode(hasScrollAction()).performScrollToKey("Roboto Flex")
        compose.onNodeWithText("Roboto Flex").assertIsDisplayed()
        compose
            .onNodeWithText(
                "Copyright 2017 The Roboto Flex Project Authors\nLicensed under the SIL Open Font License 1.1",
            ).assertIsDisplayed()
        compose
            .onNodeWithText("https://github.com/TypeNetwork/Roboto-Flex")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle {
            assertEquals(listOf("https://github.com/TypeNetwork/Roboto-Flex"), opened)
        }
        compose.onNodeWithText("Лицензии FOSS").assertIsDisplayed()
    }
}

private const val AUTHOR_URL_SAMPLE = "https://solthariel.github.io/"
