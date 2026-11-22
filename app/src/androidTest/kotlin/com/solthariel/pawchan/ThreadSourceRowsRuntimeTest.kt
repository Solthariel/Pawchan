package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import com.solthariel.pawchan.threads.ThreadSourceUi
import com.solthariel.pawchan.threads.ThreadsSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadSourceRowsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSectionsKeepSiteMarksTitlesAndActionsOnTheSameGrid() {
        val rows =
            listOf(
                row("dvach", "Страница", ThreadSourceUi("2ch", R.drawable.forum_2ch)),
                row("4chan", "Избранное", ThreadSourceUi("4chan", R.drawable.forum_4chan)),
                row("unknown", "Ответ", ThreadSourceUi("unknown")),
            )
        var opened: ThreadAddress? = null
        var closed: ThreadAddress? = null
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    ThreadsSection.entries.forEachIndexed { index, section ->
                        val row = rows[index]
                        ThreadRow(
                            row,
                            index == 0,
                            { opened = row.address },
                            {},
                            ThreadRowCloseAction(section.title) { closed = row.address },
                            40.dp,
                        )
                    }
                }
            }
        }
        val density = compose.density.density
        rows.forEach { row ->
            val icon = compose.onNodeWithContentDescription("Форум: ${row.source.title}", useUnmergedTree = true)
            val bounds = icon.fetchSemanticsNode().boundsInRoot
            val title = compose.onNodeWithText(row.title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(16f * density, bounds.left, 0.5f)
            assertEquals(24f * density, bounds.width, 0.5f)
            assertEquals(24f * density, bounds.height, 0.5f)
            assertEquals(16f * density, title.left - bounds.right, 0.5f)
            assertEquals(bounds.center.y, title.center.y, 0.5f)
            compose.onNodeWithText(row.title).performClick()
            compose.runOnIdle { assertEquals(row.address, opened) }
        }
        compose.onNodeWithContentDescription("Страницы").performClick()
        compose.runOnIdle { assertEquals(rows.first().address, closed) }
    }

    @Test fun unknownSourceUsesItsOwnIdentityInsteadOfAnotherForumsBrand() {
        val row = row("unknown", "Неизвестный сайт", ThreadSourceUi("unknown"))
        compose.setContent {
            MaterialTheme { ThreadRow(row, false, {}, {}) }
        }
        compose.onNodeWithContentDescription("Форум: unknown", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Форум: 2ch", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Форум: 4chan", useUnmergedTree = true).assertDoesNotExist()
    }

    private fun row(
        site: String,
        title: String,
        source: ThreadSourceUi,
    ): ThreadRowUi =
        ThreadRowUi(
            ThreadAddress(
                BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse("b"))),
                requireNotNull(ThreadId.parse(1L)),
            ),
            title,
            ThreadCounter(123),
            source = source,
        )
}
