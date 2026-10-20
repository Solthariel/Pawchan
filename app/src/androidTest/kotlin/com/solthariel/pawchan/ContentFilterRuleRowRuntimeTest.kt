package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterRuleRow
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterRuleRowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun longRuleUsesOneEllipsizedLineWithoutChangingItsText() {
        val text = "Главный новостной канал в Телеге https://t.me/example ".repeat(4)
        val rule = newContentFilterRule(defaultBoardAddress(), text)
        var opened = false
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(240.dp)) {
                    ContentFilterRuleRow(rule, 1L) { opened = true }
                }
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(rule.label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(1, results.single().lineCount)
        assertTrue(results.single().isLineEllipsized(0))
        assertEquals(text, rule.values.single())
        compose.onNodeWithText(rule.label).performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
