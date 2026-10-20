package com.solthariel.pawchan

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogBoardIdentity
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatalogHeaderRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val speed = mutableStateOf<Int?>(null)
    private val title = mutableStateOf<String?>(null)
    private val board = BoardAddress(defaultBoardAddress().site, requireNotNull(BoardId.parse("hry")))

    @Test
    fun titleKeepsTheSameTopPositionWhenSpeedArrives() {
        title.value = "Highlights of Russian-speaking youtube"
        show()
        val label = "/hry/ — ${title.value}"
        val before = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { speed.value = 1 }
        val after = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 0.1f)
        assertEquals(before.left, after.left, 0.1f)
    }

    @Test
    fun loadingCodeAndLoadedLongTitleUseTheSameBaselineSlot() {
        show()
        val before = compose.onNodeWithText("/hry/").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            title.value = "Highlights of Russian-speaking youtube"
            speed.value = 1
        }
        val after = compose.onNodeWithText("/hry/ — ${title.value}").fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 0.1f)
    }

    @Test
    fun longHryTitleIsOneEllipsizedLineWithASeparateStandardIndicator() {
        title.value = "Highlights of Russian-speaking youtube"
        show()
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("/hry/ — ${title.value}").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(results)
        }
        assertEquals(1, results.single().lineCount)
        assertTrue(results.single().isLineEllipsized(0))
        compose
            .onNodeWithTag("disclosure-indicator", useUnmergedTree = true)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                Row(Modifier.width(240.dp).height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                    CatalogBoardIdentity(board, title.value, speed.value, Modifier.weight(1f), switchable = true)
                }
            }
        }
    }
}
