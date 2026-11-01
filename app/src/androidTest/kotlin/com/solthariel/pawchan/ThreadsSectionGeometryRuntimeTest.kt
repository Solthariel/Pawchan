package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.threads.ThreadCounter
import com.solthariel.pawchan.threads.ThreadRow
import com.solthariel.pawchan.threads.ThreadRowCloseAction
import com.solthariel.pawchan.threads.ThreadRowUi
import com.solthariel.pawchan.threads.ThreadsSection
import com.solthariel.pawchan.threads.ThreadsSectionHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThreadsSectionGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSectionsAlignCountersAndCloseButtonsUnderDisclosure() {
        var closes = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    ThreadsSection.entries.forEach { section ->
                        ThreadsSectionHeader(section, true, ThreadCounter(123), {}, 40.dp)
                        ThreadRow(row(), false, {}, {}, ThreadRowCloseAction(section.title) { closes++ }, 40.dp)
                    }
                }
            }
        }
        val counts = compose.onAllNodesWithText("123", useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(6, counts.size)
        counts.forEach {
            assertEquals(
                counts
                    .first()
                    .boundsInRoot.center.x,
                it.boundsInRoot.center.x,
                0f,
            )
        }
        val arrows = compose.onAllNodesWithTag("disclosure-indicator", true).fetchSemanticsNodes()
        ThreadsSection.entries.forEachIndexed { index, section ->
            val close = compose.onNodeWithContentDescription(section.title)
            assertEquals(
                arrows[index].boundsInRoot.center.x,
                close
                    .fetchSemanticsNode()
                    .boundsInRoot.center.x,
                HALF_PIXEL_ROUNDING,
            )
            assertTrue(close.fetchSemanticsNode().boundsInRoot.width >= with(compose.density) { 48.dp.toPx() })
            close.performClick()
        }
        compose.runOnIdle { assertEquals(3, closes) }
    }

    @Test fun collapsingKeepsCounterAndArrowPositionsAndHasNoCloseAllButton() {
        val expanded = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    ThreadsSection.entries.forEach { section ->
                        ThreadsSectionHeader(
                            section,
                            expanded.value,
                            ThreadCounter(0),
                            { expanded.value = !expanded.value },
                        )
                    }
                }
            }
        }
        val before =
            compose.onAllNodesWithTag("disclosure-indicator", true).fetchSemanticsNodes().map { it.boundsInRoot }
        val counts =
            compose.onAllNodesWithText("0", useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }
        compose.onNodeWithText("Страницы").performClick()
        assertEquals(
            before,
            compose.onAllNodesWithTag("disclosure-indicator", true).fetchSemanticsNodes().map { it.boundsInRoot },
        )
        assertEquals(
            counts,
            compose.onAllNodesWithText("0", useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot },
        )
        compose.onNodeWithContentDescription("Закрыть все").assertDoesNotExist()
    }

    private fun row(): ThreadRowUi =
        ThreadRowUi(
            ThreadAddress(
                BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
                requireNotNull(ThreadId.parse(1L)),
            ),
            "Длинное название треда для проверки одинаковой разметки",
            ThreadCounter(123),
        )
}

// A 24dp icon can rasterize to an odd pixel width inside an even-width 48dp target.
private const val HALF_PIXEL_ROUNDING = 0.5f
