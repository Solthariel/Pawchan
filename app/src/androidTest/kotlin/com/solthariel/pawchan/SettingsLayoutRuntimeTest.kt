package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.backup.BackupSection
import com.solthariel.pawchan.backup.DEFAULT_BACKUP_SECTIONS
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.settings.BackupSelectionDialog
import com.solthariel.pawchan.settings.ChangelogSettings
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import com.solthariel.pawchan.settings.SettingsPageToolbar
import com.solthariel.pawchan.settings.SettingsToolbar
import com.solthariel.pawchan.settings.UsageValueRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settingsRowsUseOneCompactHeightAndOneToggleAction() {
        val checked = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Column {
                    SettingsItemRow(SettingsItem.Toggle("Имя", null, checked.value) { checked.value = it })
                    SettingsItemRow(SettingsItem.Toggle("Почта", "Поле отправки", false) {})
                }
            }
        }
        compose.onNode(hasText("Имя") and hasClickAction()).assertHeightIsEqualTo(56.dp).performClick()
        compose.onNode(hasText("Имя") and isToggleable()).assertIsOn()
        compose.onNode(hasText("Почта") and hasClickAction()).assertHeightIsEqualTo(72.dp)
    }

    @Test
    fun backupChoicesAreCompactAndIndependentlySelectable() {
        val selected = mutableStateOf(DEFAULT_BACKUP_SECTIONS)
        compose.setContent {
            MaterialTheme {
                BackupSelectionDialog(false, selected.value, { selected.value = it }, {}, {})
            }
        }
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
                useUnmergedTree = true,
            ).assertCountEquals(1)
        compose.onAllNodes(isToggleable()).assertCountEquals(BackupSection.entries.size)
        val history = compose.onNode(hasText("История посещений") and isToggleable())
        history.assertHeightIsAtLeast(48.dp).performClick().assertIsOn()
        compose.runOnIdle { assertTrue(BackupSection.HISTORY in selected.value) }
    }

    @Test
    fun backupRestoreUsesOneScrollContainerAndConfirmsSelection() {
        val selected = mutableStateOf(BackupSection.entries.toSet())
        var confirmed = false
        compose.setContent {
            MaterialTheme {
                BackupSelectionDialog(true, selected.value, { selected.value = it }, {}, { confirmed = true })
            }
        }
        compose.onNodeWithText("Что восстановить?").assertIsDisplayed()
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
                useUnmergedTree = true,
            ).assertCountEquals(1)
        compose.onAllNodes(isToggleable()).assertCountEquals(BackupSection.entries.size)
        compose.onNodeWithText("Восстановить").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun statisticsAndSettingsToolbarsHaveIdenticalButtonBounds() {
        val statistics = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                if (statistics.value) {
                    SettingsPageToolbar("Статистика", {}) {
                        UiToolbarButton(UiIconKind.MORE, "Меню", onClick = {})
                    }
                } else {
                    SettingsToolbar("Настройки", rememberCatalogSearchState("", {}), "Поиск", {}) {
                        UiToolbarButton(UiIconKind.MORE, "Меню", onClick = {})
                    }
                }
            }
        }
        val back = compose.onNodeWithContentDescription("Назад").fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithContentDescription("Меню").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { statistics.value = true }
        compose.waitForIdle()
        assertEquals(back, compose.onNodeWithContentDescription("Назад").fetchSemanticsNode().boundsInRoot)
        assertEquals(menu, compose.onNodeWithContentDescription("Меню").fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun statisticValueSitsBesideItsLabelInsteadOfBelow() {
        compose.setContent { MaterialTheme { UsageValueRow("Открыто тредов", "123") } }
        val label = compose.onNodeWithText("Открыто тредов").fetchSemanticsNode().boundsInRoot
        val value = compose.onNodeWithText("123").fetchSemanticsNode().boundsInRoot
        assertEquals(label.center.y, value.center.y, 1f)
        assertTrue(value.left > label.right)
    }

    @Test fun changelogUsesTheSameRowHeightAndTextInsetsAsOtherActions() {
        compose.setContent {
            MaterialTheme {
                Column {
                    SettingsItemRow(SettingsItem.Action("Обычная строка", null, onClick = {}))
                    ChangelogSettings()
                }
            }
        }
        val reference = compose.onNodeWithText("Обычная строка")
        val changelog = compose.onNodeWithText("Список изменений")
        reference.assertHeightIsEqualTo(56.dp)
        changelog.assertHeightIsEqualTo(56.dp)
        val firstText = compose.onNodeWithText("Обычная строка", useUnmergedTree = true)
        val secondText = compose.onNodeWithText("Список изменений", useUnmergedTree = true)
        assertEquals(
            firstText.fetchSemanticsNode().boundsInRoot.left,
            secondText.fetchSemanticsNode().boundsInRoot.left,
            1f,
        )
        changelog.performClick()
        compose.onNodeWithContentDescription("Назад к настройкам").assertExists()
    }
}
