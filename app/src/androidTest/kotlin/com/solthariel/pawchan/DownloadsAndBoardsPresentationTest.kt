package com.solthariel.pawchan

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardListDimensions
import com.solthariel.pawchan.boards.BoardsAddressBindings
import com.solthariel.pawchan.boards.BoardsNavigation
import com.solthariel.pawchan.boards.BoardsPresentationCache
import com.solthariel.pawchan.boards.BoardsScreen
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.prepareBoardsList
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.core.ui.rememberFloatingTopChromeState
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.downloads.DownloadActionHandlers
import com.solthariel.pawchan.downloads.DownloadActions
import com.solthariel.pawchan.downloads.DownloadList
import com.solthariel.pawchan.downloads.DownloadOrigin
import com.solthariel.pawchan.downloads.DownloadRecord
import com.solthariel.pawchan.downloads.DownloadStatus
import com.solthariel.pawchan.downloads.DownloadThumbnail
import com.solthariel.pawchan.library.LibraryToolbar
import com.solthariel.pawchan.library.LibraryToolbarActions
import com.solthariel.pawchan.library.LibraryToolbarState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DownloadsAndBoardsPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun libraryTitleDoesNotAppendCount() {
        compose.setContent {
            MaterialTheme { LibraryToolbar(LibraryToolbarState("История", 21, null, false), LibraryToolbarActions({}, {})) }
        }
        compose.onNodeWithText("История").assertIsDisplayed()
        compose.onNodeWithText("История / 21").assertDoesNotExist()
    }

    @Test fun successfulThumbnailOpensTheSavedFileEvenIfPreviewUnavailable() {
        var opened = 0
        val record =
            DownloadRecord(
                "1",
                "https://example.test/a.jpg",
                "content://missing/a",
                "a.jpg",
                DownloadOrigin(null, null),
                0L,
                DownloadStatus.SUCCEEDED,
            )
        compose.setContent {
            MaterialTheme {
                val cache = remember { LruCache<String, Bitmap>(4) }
                DownloadThumbnail(record, cache) { opened++ }
            }
        }
        compose.onNodeWithContentDescription("Предпросмотр недоступен").performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test fun initialDownloadsStateDoesNotClaimHistoryIsEmpty() {
        compose.setContent {
            MaterialTheme { DownloadList(null, LruCache(4), {}, {}) }
        }
        compose.onNodeWithText("Загрузка…").assertIsDisplayed()
        compose.onNodeWithText("Загрузок нет").assertDoesNotExist()
    }

    @Test fun downloadHistoryReadErrorIsNotReportedAsEmpty() {
        compose.setContent {
            MaterialTheme { DownloadList(null, LruCache(4), {}, {}, loadError = true) }
        }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
        compose.onNodeWithText("Загрузок нет").assertDoesNotExist()
    }

    @Test fun downloadsMenuUsesSamePostLabelForAllSources() {
        val record =
            DownloadRecord(
                "1",
                "https://example.test/a.jpg",
                "content://file/a",
                "a.jpg",
                DownloadOrigin("thread/dvach/b/12", null, 44),
                0L,
                DownloadStatus.SUCCEEDED,
            )
        val current = mutableStateOf(record)
        var threadActions = 0
        var archiveActions = 0
        val actions = DownloadActionHandlers({}, {}, { threadActions++ }, { archiveActions++ }, {}, {})
        compose.setContent { MaterialTheme { DownloadActions(current.value, actions) } }
        compose.onNodeWithText("Перейти к посту").performClick()
        compose.runOnIdle {
            assertEquals(1, threadActions)
            current.value = record.copy(origin = record.origin.copy(postId = null))
        }
        compose.onNodeWithText("Перейти к посту").performClick()
        compose.runOnIdle {
            assertEquals(2, threadActions)
            current.value = record.copy(origin = DownloadOrigin(null, 45))
        }
        compose.onNodeWithText("Перейти к посту").performClick()
        compose.runOnIdle { assertEquals(1, archiveActions) }
        compose.onNodeWithText("Открыть тред").assertDoesNotExist()
        compose.onNodeWithText("Открыть пост").assertDoesNotExist()
    }

    @Test fun loadingEachForumUsesOnlyItsOwnPreparedList() {
        val first = requireNotNull(SiteId.parse("dvach"))
        val second = requireNotNull(SiteId.parse("4chan"))
        val selected = mutableStateOf(first)
        val dimensions = BoardListDimensions(48.dp, 96.dp, 48.dp)
        val entries =
            mutableStateMapOf(
                "dvach\u0000false\u0000" to content(first, "Тематика").prepareBoardsList("", dimensions, 1f, categoryFilter = ""),
                "4chan\u0000false\u0000" to content(second, "Творчество").prepareBoardsList("", dimensions, 1f, categoryFilter = ""),
            )
        compose.setContent {
            MaterialTheme {
                val cache = BoardsPresentationCache(entries, rememberFloatingTopChromeState())
                key(selected.value) {
                    BoardsScreen(
                        BoardsUiState.Loading,
                        BoardsNavigation({}, {}),
                        BoardsActions({ _, _ -> }, { _, _ -> }, {}),
                        BoardsAddressBindings(selected.value, { null }, {}),
                        presentationCache = cache,
                    )
                }
            }
        }
        compose.onNodeWithText("/b/").assertIsDisplayed()
        compose.runOnIdle { selected.value = second }
        compose.onNodeWithText("/a/").assertIsDisplayed()
        compose.onNodeWithText("/b/").assertDoesNotExist()
    }

    private fun content(
        site: SiteId,
        category: String,
    ): BoardsUiState.Content {
        val board =
            BoardDirectoryEntry(
                BoardAddress(
                    site,
                    requireNotNull(
                        BoardId.parse(
                            if (site.value ==
                                "dvach"
                            ) {
                                "b"
                            } else {
                                "a"
                            },
                        ),
                    ),
                ),
                "test",
                category,
                0,
                true,
            )
        return BoardsUiState.Content(listOf(board), emptySet(), emptyMap(), emptySet(), emptySet(), false)
    }
}
