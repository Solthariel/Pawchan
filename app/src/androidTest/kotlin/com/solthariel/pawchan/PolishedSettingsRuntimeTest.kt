package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.settings.ChangelogList
import com.solthariel.pawchan.settings.ChangelogRelease
import com.solthariel.pawchan.settings.ChangelogSettings
import com.solthariel.pawchan.settings.ProxyCatalogEditor
import com.solthariel.pawchan.settings.ProxyEditorState
import com.solthariel.pawchan.settings.ProxyResourceChoices
import com.solthariel.pawchan.settings.SettingsPageToolbar
import com.solthariel.pawchan.settings.newProxy
import com.solthariel.pawchan.update.ReleaseNotesSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PolishedSettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changelogSeparatesReleasesNotSections() {
        val section = ReleaseNotesSection("Исправления", listOf("Первое", "Второе"))
        val releases =
            listOf(
                ChangelogRelease(10, "1.0", "01.01.2026", listOf(section)),
                ChangelogRelease(9, "0.9", "01.12.2025", listOf(section)),
            )
        compose.setContent { MaterialTheme { ChangelogList(releases, Modifier) } }
        compose.onNodeWithTag("changelog-divider-10").assertDoesNotExist()
        val divider = compose.onNodeWithTag("changelog-divider-9").fetchSemanticsNode().boundsInRoot
        val first = compose.onNodeWithTag("changelog-version-10").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("changelog-version-9").fetchSemanticsNode().boundsInRoot
        assertTrue(first.bottom < divider.top && divider.bottom < second.top)
    }

    @Test fun versionAndNumberShareOneLineWithChangelogAction() {
        compose.setContent {
            MaterialTheme { Column(Modifier.width(600.dp)) { ChangelogSettings(rememberBundledTestChangelogHistory(), true) } }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        val version = compose.onNodeWithTag("about-version", useUnmergedTree = true)
        version.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val label = version.fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Список изменений").fetchSemanticsNode().boundsInRoot
        assertEquals(1, layouts.single().lineCount)
        assertTrue(label.right <= button.left)
        assertTrue(label.top < button.bottom && button.top < label.bottom)
    }

    @Test fun resourceRowsHaveRoomAndRemainToggleable() {
        compose.setContent {
            MaterialTheme { ProxyResourceChoices(remember { ProxyEditorState(newProxy()) }) }
        }
        val card = compose.onNodeWithTag("proxy-resources").fetchSemanticsNode().boundsInRoot
        val firstRow = compose.onNodeWithContentDescription("Ресурс 2ch")
        val lastRow = compose.onNodeWithContentDescription("Ресурс GitHub")
        firstRow.assertHeightIsAtLeast(56.dp).performClick().assertIsOn()
        lastRow.assertHeightIsAtLeast(56.dp)
        val first = compose.onNodeWithTag("proxy-checkbox-DVACH", useUnmergedTree = true)
        val last = compose.onNodeWithTag("proxy-checkbox-GITHUB", useUnmergedTree = true)
        val a = first.fetchSemanticsNode().boundsInRoot
        val b = last.fetchSemanticsNode().boundsInRoot
        assertTrue(a.top > card.top && a.right < card.right)
        assertTrue(b.bottom < card.bottom && b.right < card.right)
    }

    @Test fun proxyEditorUsesStandardToolbarButtonGeometry() {
        var editor by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                if (editor) {
                    ProxyCatalogEditor(remember { newProxy() }, emptyList(), "2ch.su", {}, {}, {})
                } else {
                    SettingsPageToolbar("Прокси", {}, {})
                }
            }
        }
        val reference = compose.onNodeWithContentDescription("Назад").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { editor = true }
        val actual = compose.onNodeWithContentDescription("Назад").fetchSemanticsNode().boundsInRoot
        assertEquals(reference.left, actual.left, 1f)
        assertEquals(reference.width, actual.width, 1f)
        assertEquals(reference.height, actual.height, 1f)
        compose.onNodeWithContentDescription("Сохранить").assertExists()
    }
}
