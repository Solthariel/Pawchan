package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.solthariel.pawchan.settings.ChangelogDownload
import com.solthariel.pawchan.settings.ChangelogHistoryController
import com.solthariel.pawchan.settings.ChangelogHistoryRepository
import com.solthariel.pawchan.settings.ChangelogSettings
import com.solthariel.pawchan.settings.decodeRemoteChangelog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun newlyFoundVersionAppearsInTheOpenDialogAndSurvivesReopening() {
        val published = AtomicBoolean(false)
        val calls = AtomicInteger()
        val visit = Any()
        lateinit var history: ChangelogHistoryController
        val installed = BuildConfig.VERSION_CODE
        val next = installed + 1
        val nextName = "0.1.999"
        compose.setContent {
            val scope = rememberCoroutineScope()
            history =
                remember {
                    val bundled = historyJson(installed, BuildConfig.VERSION_NAME)
                    val repository =
                        ChangelogHistoryRepository(null, { decodeRemoteChangelog(bundled) }) { _, _ ->
                            calls.incrementAndGet()
                            val body =
                                if (published.get()) {
                                    val newBody = historyJson(next, nextName)
                                    val newRow = newBody.substringAfter("[").substringBeforeLast("]")
                                    bundled.replace("[", "[$newRow,", ignoreCase = false)
                                } else {
                                    bundled
                                }
                            ChangelogDownload(body, null)
                        }
                    ChangelogHistoryController(repository, scope)
                }
            LaunchedEffect(history) { history.enter(visit) }
            MaterialTheme { ChangelogSettings(history) }
        }
        compose.waitUntil(timeoutMillis = 10_000) { calls.get() == 1 }
        compose.onNodeWithText("Список изменений").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(BuildConfig.VERSION_NAME).fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            assertTrue("Opening must reuse the entry request", calls.get() == 1)
            published.set(true)
            history.updateFound(next.toLong())
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(nextName).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(nextName).assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад к настройкам").performClick()
        compose.onNodeWithText("Список изменений").performClick()
        compose.onNodeWithText(nextName).assertIsDisplayed()
    }

    private fun historyJson(
        code: Int,
        name: String,
    ): String =
        """{"schema":1,"releases":[{"code":$code,"name":"$name","date":"21.10.2026",
            "notes":"Исправления\n• Проверка обновления истории"}]}"""

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
