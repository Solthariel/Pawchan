package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.catalog.CatalogThreadContentUi
import com.solthariel.pawchan.catalog.CatalogThreadFlagsUi
import com.solthariel.pawchan.catalog.CatalogThreadStatisticsUi
import com.solthariel.pawchan.catalog.CatalogThreadTextUi
import com.solthariel.pawchan.catalog.CatalogThreadUi
import com.solthariel.pawchan.catalog.rememberCatalogThreadMenu
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogContentRuleMenuUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun catalogRuleItemImmediatelyFollowsShareAndReceivesTheSelectedSourceThread() {
        val thread =
            CatalogThreadUi(
                ThreadAddress(defaultBoardAddress(), checkNotNull(ThreadId.parse(42))),
                CatalogThreadContentUi(null, CatalogThreadTextUi("голова дай денег"), null, null, false),
                emptyList(),
                CatalogThreadFlagsUi(false, false, false),
                CatalogThreadStatisticsUi(0, 0, 0L, false),
            )
        var selected: CatalogThreadUi? = null
        compose.setContent {
            MaterialTheme {
                val menu = rememberCatalogThreadMenu({}, null) { selected = it }
                TextButton(onClick = { menu.onLongClick(thread) }) { Text("Открыть меню") }
            }
        }
        compose.onNodeWithText("Открыть меню").performClick()
        val share = compose.onNodeWithText("Поделиться ссылкой").fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithText("Добавить правило").fetchSemanticsNode().boundsInRoot
        assertTrue(add.top >= share.bottom)
        compose.onNodeWithText("Добавить правило").performClick()
        compose.runOnIdle { assertEquals(thread, selected) }
    }
}
