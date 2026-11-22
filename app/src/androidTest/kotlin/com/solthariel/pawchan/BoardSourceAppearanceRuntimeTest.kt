package com.solthariel.pawchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.boards.BoardSourcePicker
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardSourceAppearanceRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val first = requireNotNull(SiteId.parse("dvach"))
    private val second = requireNotNull(SiteId.parse("4chan"))
    private val selected = mutableStateOf("dvach")
    private val directory =
        object : BoardDirectoryRepository {
            override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
                DomainOutcome.Success(emptyList())
        }

    @Test fun selectingAnySourceDoesNotResizeTabs() {
        showPicker()
        val tabs = listOf("dvach", "4chan")
        val widths = tabs.map(::tabWidth)
        for (source in tabs) {
            compose.runOnIdle { selected.value = source }
            compose.onNodeWithText(if (source == "dvach") "2ch" else "4chan").assertIsSelected()
            tabs.forEachIndexed { index, tab -> assertEquals("Width of $tab", widths[index], tabWidth(tab), 0.5f) }
        }
    }

    private fun showPicker() {
        val sources =
            listOf(
                ForumBoardSource(first, "2ch", directory, icon = R.drawable.forum_2ch),
                ForumBoardSource(second, "4chan", directory, icon = R.drawable.forum_4chan),
            )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.Magenta, onSurfaceVariant = Color.Cyan)) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    BoardSourcePicker(
                        sources,
                        if (selected.value == "4chan") second else first,
                        archiveSelected = selected.value == "arhivach",
                    ) { selected.value = it.value }
                }
            }
        }
    }

    private fun tabWidth(site: String): Float =
        compose
            .onNodeWithText(if (site == "dvach") "2ch" else "4chan")
            .fetchSemanticsNode()
            .boundsInRoot.width
}
