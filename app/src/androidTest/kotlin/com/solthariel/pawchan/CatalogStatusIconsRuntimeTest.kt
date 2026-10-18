package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.catalog.CatalogThreadStatusIcons
import org.junit.Rule
import org.junit.Test

class CatalogStatusIconsRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun allThreeStatusesHaveDescriptionsWithoutAVisibleEndlessLabel() {
        compose.setContent { MaterialTheme { CatalogThreadStatusIcons(pinned = true, closed = true, endless = true) } }
        compose.onNodeWithContentDescription("закреплён").assertIsDisplayed()
        compose.onNodeWithContentDescription("закрыт").assertIsDisplayed()
        compose.onNodeWithContentDescription("бесконечный").assertIsDisplayed()
        compose.onNodeWithText("бесконечный").assertDoesNotExist()
    }

    @Test
    fun absentFlagsDoNotCreateAStatus() {
        compose.setContent { MaterialTheme { CatalogThreadStatusIcons(pinned = false, closed = false, endless = false) } }
        compose.onNodeWithContentDescription("закреплён").assertDoesNotExist()
        compose.onNodeWithContentDescription("закрыт").assertDoesNotExist()
        compose.onNodeWithContentDescription("бесконечный").assertDoesNotExist()
    }
}
