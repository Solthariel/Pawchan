package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.settings.CacheClearDialog
import com.solthariel.pawchan.settings.FilterOptionGroup
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import com.solthariel.pawchan.settings.SettingsRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MaterialSettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchAndItsLabelShareOneToggleTargetAndDispatchOnlyOnce() {
        var checked by mutableStateOf(false)
        var calls = 0
        compose.setContent {
            MaterialTheme {
                SettingsItemRow(
                    SettingsItem.Toggle("Переключение", null, checked) {
                        checked = it
                        calls++
                    },
                )
            }
        }
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        val row = compose.onNode(hasText("Переключение") and isToggleable())
        row
            .assertHeightIsAtLeast(48.dp)
            .assertIsOff()
            .performClick()
            .assertIsOn()
        row.performTouchInput {
            click(
                androidx.compose.ui.geometry
                    .Offset(width - 20f, center.y),
            )
        }
        row.assertIsOff()
        compose.runOnIdle { assertEquals(2, calls) }
    }

    @Test fun informationalListItemHasNoClickAction() {
        compose.setContent {
            MaterialTheme { Column { SettingsRow("2ch.su", "Доступно", onClick = null) { Text("Проверено") } } }
        }
        compose.onNodeWithText("2ch.su").assertHasNoClickAction()
    }

    @Test fun materialConfirmationCancelAndConfirmKeepTheirSeparateActions() {
        var open by mutableStateOf(true)
        var clears = 0
        compose.setContent {
            MaterialTheme {
                if (open) {
                    CacheClearDialog({ open = false }, {
                        clears++
                        open = false
                    })
                }
            }
        }
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle {
            assertEquals(0, clears)
            open = true
        }
        compose.onNodeWithText("Очистить").performClick()
        compose.runOnIdle { assertEquals(1, clears) }
    }

    @Test fun largeTextOptionsKeepTheirFullLabelsAndRemainSelectable() {
        var selected by mutableStateOf("phrase")
        val labels = listOf("Фраза", "Любое слово", "Все слова")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.8f)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        FilterOptionGroup(
                            "Как искать",
                            listOf("phrase" to labels[0], "any" to labels[1], "all" to labels[2]),
                            selected,
                        ) { selected = it }
                    }
                }
            }
        }
        labels.forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose
                .onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            compose.runOnIdle {
                assertTrue(layouts.isNotEmpty())
                val layout = layouts.first()
                assertFalse(
                    "Clipped option: $label, paragraphWidth=${layout.multiParagraph.width}, " +
                        "paragraphHeight=${layout.multiParagraph.height}, " +
                        "heightOverflow=${layout.didOverflowHeight}, widthOverflow=${layout.didOverflowWidth}, " +
                        "layout=$layout",
                    layout.hasVisualOverflow,
                )
            }
        }
        compose.onNodeWithText("Все слова").performClick()
        compose.runOnIdle { assertEquals("all", selected) }
    }
}
