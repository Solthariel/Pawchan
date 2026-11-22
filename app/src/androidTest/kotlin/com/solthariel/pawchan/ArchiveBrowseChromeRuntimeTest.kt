package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.arhivach.ArchiveBrowseModel
import com.solthariel.pawchan.arhivach.ArchiveBrowseState
import com.solthariel.pawchan.arhivach.ArchiveLoadState
import com.solthariel.pawchan.arhivach.ArchiveOpenThreadState
import com.solthariel.pawchan.arhivach.ArchivePaginationState
import com.solthariel.pawchan.arhivach.ArchiveSearchState
import com.solthariel.pawchan.arhivach.ArchiveTagSuggestions
import com.solthariel.pawchan.arhivach.ArchiveToolbar
import com.solthariel.pawchan.arhivach.ArchiveToolbarActions
import com.solthariel.pawchan.arhivach.ArchiveToolbarSearchBindings
import com.solthariel.pawchan.arhivach.ArchiveWordSearch
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ArchiveBrowseChromeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun archiveTagSuggestionsUseFullWidthRowsAndSelectTag() {
        val tag = ArchiveTag(2, "/mobi/ - Мобильные устройства и приложения")
        val model = ArchiveBrowseModel(EmptyArchiveBrowser())
        var closed = false
        val state =
            ArchiveBrowseState(
                search = ArchiveSearchState(query = "mobi", suggestions = listOf(tag, ArchiveTag(3, "mobile-phone"))),
            )
        compose.setContent {
            MaterialTheme {
                ArchiveTagSuggestions(state, model) { closed = true }
            }
        }

        val suggestions = compose.onNodeWithTag("archive-tag-suggestions")
        val firstRow = compose.onNodeWithTag("archive-tag-row-2")
        suggestions.assertHeightIsEqualTo(96.dp)
        firstRow.assertHeightIsEqualTo(48.dp)
        assertEquals(
            suggestions.fetchSemanticsNode().boundsInRoot.width,
            firstRow.fetchSemanticsNode().boundsInRoot.width,
            0.1f,
        )
        compose.onNodeWithText(tag.title).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf(tag), model.state.value.tags)
            assertTrue(closed)
        }
    }

    @Test fun archiveThreadToolbarUsesStandardActionsAndWorkingSearch() {
        val model = ArchiveBrowseModel(EmptyArchiveBrowser())
        val thread = ArchiveThread(55, "Тест", emptyList())
        val state = ArchiveBrowseState(openThread = ArchiveOpenThreadState(55, thread, galleryEnabled = true))
        val lookup = CatalogBoardLookupBindings(false, "", {}, {}, {}, suggestions = {})
        var searchVisible by mutableStateOf(false)
        var query by mutableStateOf("")
        var galleryClicks = 0
        compose.setContent {
            MaterialTheme {
                ArchiveToolbar(
                    state,
                    ArchiveToolbarActions(model, {}) { galleryClicks++ },
                    ArchiveToolbarSearchBindings(lookup, ArchiveWordSearch(searchVisible, query, { searchVisible = it }, { query = it })),
                    galleryEnabled = true,
                )
            }
        }
        val labels = listOf("Назад", "Галерея", "Поиск", "Обновить", "Меню треда")
        val positions =
            labels.map {
                compose
                    .onNodeWithContentDescription(it)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot.left
            }
        assertTrue(positions.zipWithNext().all { (left, right) -> left < right })
        compose.onNodeWithText("Тред №55").assertDoesNotExist()
        compose.onNodeWithContentDescription("Меню треда").performClick()
        compose.onNodeWithText("Копировать ссылку").assertIsDisplayed()
        compose.onNodeWithText("Копировать ссылку").performClick()
        compose.onNodeWithContentDescription("Галерея").performClick()
        compose.runOnIdle { assertEquals(1, galleryClicks) }
        compose.onNodeWithContentDescription("Поиск").performClick()
        compose.onNodeWithContentDescription("Поиск в треде").performTextReplacement("текст")
        compose.runOnIdle { assertEquals("текст", query) }
        compose.onNodeWithContentDescription("Закрыть поиск").performClick()
        compose.runOnIdle {
            assertEquals("", query)
            assertTrue(!searchVisible)
        }
    }

    @Test fun archivePageSearchUsesTheSharedSearchToolbar() {
        val model = ArchiveBrowseModel(EmptyArchiveBrowser())
        val state = ArchiveBrowseState(pagination = ArchivePaginationState(page = 2), list = ArchiveThreadPage(emptyList(), hasNext = true))
        val lookup = CatalogBoardLookupBindings(false, "", {}, {}, {}, suggestions = {})
        val searchBindings = ArchiveToolbarSearchBindings(lookup, ArchiveWordSearch(true, "", {}, {}))
        compose.setContent {
            MaterialTheme {
                ArchiveToolbar(state, ArchiveToolbarActions(model, {}) {}, searchBindings)
            }
        }

        compose.onNodeWithContentDescription("Закрыть поиск").assertIsDisplayed()
        compose.onNodeWithText("Поиск…").assertIsDisplayed()
        compose.onNodeWithContentDescription("Обновить").assertIsDisplayed()
        compose.onNodeWithContentDescription("Меню страниц").assertIsDisplayed()
    }

    @Test fun archivePagingIsAvailableFromTheToolbarMenu() {
        val model = ArchiveBrowseModel(EmptyArchiveBrowser())
        val state =
            ArchiveBrowseState(
                pagination = ArchivePaginationState(page = 2),
                list = ArchiveThreadPage(emptyList(), hasNext = true, totalPages = 56_234),
            )
        val lookup = CatalogBoardLookupBindings(false, "", {}, {}, {}, suggestions = {})
        val searchBindings = ArchiveToolbarSearchBindings(lookup, ArchiveWordSearch(false, "", {}, {}))
        compose.setContent {
            MaterialTheme {
                ArchiveToolbar(state, ArchiveToolbarActions(model, {}) {}, searchBindings)
            }
        }

        compose.onNodeWithContentDescription("Назад").assertExists()
        compose.onNodeWithContentDescription("Поиск по словам").assertExists()
        compose.onNodeWithContentDescription("Обновить").assertExists()
        val searchLeft =
            compose
                .onNodeWithContentDescription("Поиск по словам")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        val refreshLeft =
            compose
                .onNodeWithContentDescription("Обновить")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        val menuLeft =
            compose
                .onNodeWithContentDescription("Меню страниц")
                .fetchSemanticsNode()
                .boundsInRoot
                .left
        assertTrue(searchLeft < refreshLeft && refreshLeft < menuLeft)
        compose.onNodeWithContentDescription("Меню страниц").performClick()
        compose.onNodeWithText("3 / 56234").assertExists()
        val pageItem = compose.onNodeWithTag("archive-page-current")
        pageItem.assertHasClickAction()
        val itemBounds = pageItem.fetchSemanticsNode().boundsInRoot
        val pageLabelBounds =
            compose
                .onNodeWithText("3 / 56234", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(itemBounds.center.x, pageLabelBounds.center.x, 2f)
        compose.onNodeWithTag("archive-page-previous").assertDoesNotExist()
        compose.onNodeWithTag("archive-page-next").assertDoesNotExist()
        pageItem.performClick()
        compose.onNodeWithTag("archive-page-number-input").performTextReplacement("42")
        compose.onNodeWithText("Перейти").performClick()
        compose.runOnIdle { assertEquals(41, model.state.value.page) }
    }

    @Test fun archivePageMenuHidesTheNumberWhileLoading() {
        val model = ArchiveBrowseModel(EmptyArchiveBrowser())
        val lookup = CatalogBoardLookupBindings(false, "", {}, {}, {}, suggestions = {})
        val searchBindings = ArchiveToolbarSearchBindings(lookup, ArchiveWordSearch(false, "", {}, {}))
        val loadingState = ArchiveBrowseState(pagination = ArchivePaginationState(page = 0), load = ArchiveLoadState(loading = true))
        compose.setContent {
            MaterialTheme {
                ArchiveToolbar(loadingState, ArchiveToolbarActions(model, {}) {}, searchBindings)
            }
        }

        compose.onNodeWithContentDescription("Меню страниц").performClick()
        compose.onNodeWithText("Загрузка…").assertExists()
        compose.onNodeWithText("Страница 1").assertDoesNotExist()
        compose.onNodeWithText("1 / …").assertDoesNotExist()
    }
}

private class EmptyArchiveBrowser : ArchiveBrowser {
    override suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>> = DomainOutcome.Success(emptyList())

    override suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage> = DomainOutcome.Success(ArchiveThreadPage(emptyList(), hasNext = false))

    override suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread> = DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
}
