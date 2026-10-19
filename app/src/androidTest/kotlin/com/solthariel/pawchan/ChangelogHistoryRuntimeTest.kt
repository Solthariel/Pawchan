package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.solthariel.pawchan.settings.ChangelogSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChangelogHistoryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun bundledHistoryShowsInstalledVersionAndCanScrollToFirstRelease() {
        compose.setContent { MaterialTheme { ChangelogSettings() } }
        compose.onNodeWithText("Список изменений").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(BuildConfig.VERSION_NAME).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(BuildConfig.VERSION_NAME).assertIsDisplayed()
        compose.onAllNodesWithText("Установлена").assertCountEquals(1)
        assertInstalledReleaseMetadataIsInline()
        compose.onNodeWithTag("changelog-list").performScrollToNode(hasText("0.1.0"))
        compose.onNodeWithText("0.1.0").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад к настройкам").performClick()
        compose.onAllNodesWithText("Установлена").assertCountEquals(0)
    }

    private fun assertInstalledReleaseMetadataIsInline() {
        val code = BuildConfig.VERSION_CODE
        val version = compose.onNodeWithTag("changelog-version-label-$code").fetchSemanticsNode().boundsInRoot
        val date = compose.onNodeWithTag("changelog-date-$code").fetchSemanticsNode().boundsInRoot
        val installed = compose.onNodeWithTag("changelog-installed-$code").fetchSemanticsNode().boundsInRoot
        val heading = compose.onNodeWithTag("changelog-version-$code").fetchSemanticsNode().boundsInRoot
        assertTrue("Date must follow the version", date.left > version.right)
        assertTrue("Installed label must follow the date", installed.left > date.right)
        assertTrue(
            "Version and date must share a row",
            maxOf(version.top, date.top) < minOf(version.bottom, date.bottom),
        )
        assertTrue(
            "Date and installed label must share a row",
            maxOf(date.top, installed.top) < minOf(date.bottom, installed.bottom),
        )
        assertTrue("Installed label must fit within the heading", installed.right <= heading.right)
    }
}
