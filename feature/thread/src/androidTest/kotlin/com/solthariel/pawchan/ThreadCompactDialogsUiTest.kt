package com.solthariel.pawchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.thread.PostPreviewBindings
import com.solthariel.pawchan.thread.PostPreviewPresentation
import com.solthariel.pawchan.thread.PostPreviewState
import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.PostPreviewWindow
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAppearance
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.ThreadReportController
import com.solthariel.pawchan.thread.ThreadReportDialog
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.withPostNavigation
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadCompactDialogsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun replyPreviewUsesWideCardsWithoutHeadingOrCloseButton() {
        val posts = listOf(previewPost(2, "Первый ответ"), previewPost(3, "Второй ответ с более длинным комментарием"))
        val content =
            ThreadUiState
                .Content(
                    1,
                    null,
                    posts,
                    ThreadContentFlags(false),
                    ThreadContentPresentation(false),
                    ThreadContentStatistics(),
                ).withPostNavigation()
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                PostPreviewWindow(
                    PostPreviewState(content, posts),
                    PostPreviewTarget(null, 1, replies = true),
                    rememberPreviewPresentation(),
                    PostPreviewBindings({}, {}, {}, {}, {}),
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText("Первый ответ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Второй ответ с более длинным комментарием", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Ответы на #1").assertDoesNotExist()
        compose.onNodeWithText("Закрыть").assertDoesNotExist()
        saveThreadChromeScreenshot("replies")
    }

    @Test
    fun actualFlatPreviewHasDividersOnlyBetweenPosts() {
        val first = previewPost(2, "Первый ответ")
        val second = previewPost(3, "Второй ответ")
        val posts = mutableStateOf(listOf(first))
        val bubbles = mutableStateOf(false)
        val content =
            ThreadUiState
                .Content(
                    1,
                    null,
                    listOf(first, second),
                    ThreadContentFlags(false),
                    ThreadContentPresentation(false),
                    ThreadContentStatistics(),
                ).withPostNavigation()
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                val preferences = PostDisplayPreferences(bubblePosts = bubbles.value)
                CompositionLocalProvider(LocalPostDisplayPreferences provides preferences) {
                    PostPreviewWindow(
                        PostPreviewState(content, posts.value),
                        PostPreviewTarget(null, 1, replies = true),
                        rememberPreviewPresentation(),
                        PostPreviewBindings({}, {}, {}, {}, {}),
                        {},
                        {},
                    )
                }
            }
        }
        compose.onAllNodesWithTag("post-list-divider", useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { posts.value = listOf(first, second) }
        compose.onAllNodesWithTag("post-list-divider", useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { bubbles.value = true }
        compose.onAllNodesWithTag("post-list-divider", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun closingRepliesRepeatedlyPreservesUnderlyingListOffsetAndBounds() {
        lateinit var list: androidx.compose.foundation.lazy.LazyListState
        val opened = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            list =
                androidx.compose.foundation.lazy
                    .rememberLazyListState(0, 7)
            val presentation = rememberPreviewPresentation()
            ThreadTestTheme(ThemePreference.DARK) {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier
                        .fillMaxSize(),
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(state = list) {
                        items(30) { index ->
                            androidx.compose.material3.TextButton(onClick = { opened.value = true }) {
                                androidx.compose.material3.Text("Пост $index")
                            }
                        }
                    }
                    if (opened.value) {
                        PostPreviewWindow(
                            PostPreviewState(),
                            PostPreviewTarget(null, 1, replies = true),
                            presentation,
                            PostPreviewBindings({ opened.value = false }, {}, {}, {}, {}),
                            {},
                            {},
                        )
                    }
                }
            }
        }
        val before = compose.onNodeWithText("Пост 0").fetchSemanticsNode().boundsInRoot
        repeat(3) {
            compose.onNodeWithText("Пост 1").performClick()
            compose.waitForIdle()
            androidx.test.espresso.Espresso
                .pressBack()
            compose.waitForIdle()
            assertEquals(before, compose.onNodeWithText("Пост 0").fetchSemanticsNode().boundsInRoot)
            compose.runOnIdle {
                assertEquals(0, list.firstVisibleItemIndex)
                assertEquals(7, list.firstVisibleItemScrollOffset)
            }
        }
    }

    @Test
    fun reportDialogHasShortTitleAndCannotSubmitAnEmptyReason() {
        var closed = 0
        compose.setContent {
            val scope = rememberCoroutineScope()
            val address =
                ThreadAddress(
                    BoardAddress(requireNotNull(SiteId.parse("2ch")), requireNotNull(BoardId.parse("b"))),
                    requireNotNull(ThreadId.parse(1)),
                )
            val controller = remember { ThreadReportController(scope, address, null) }
            ThreadTestTheme(ThemePreference.DARK) {
                ThreadReportDialog(controller, setOf(1, 2, 3, 4)) { closed++ }
            }
        }
        compose.onNodeWithText("Жалоба на посты: 4").assertIsDisplayed()
        compose.onNodeWithText("Отправить").assertIsNotEnabled()
        saveThreadChromeScreenshot("report")
        compose.onNodeWithText("Отмена").performClick()
        assertEquals(1, closed)
    }

    @Composable
    private fun rememberPreviewPresentation(): PostPreviewPresentation {
        val context = LocalContext.current
        val client = remember { OkHttpClient() }
        val thumbnails = remember(context, client) { ThumbnailLoader(context, client) }
        DisposableEffect(thumbnails) {
            onDispose {
                thumbnails.close()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
        return PostPreviewPresentation(ThreadAppearance(), thumbnails)
    }

    private fun previewPost(
        id: Long,
        text: String,
    ): ThreadPostUi {
        val document =
            PostDocument(
                listOf(
                    PostTextRun(">>1\n", link = PostLink.Reference(requireNotNull(PostId.parse(1)))),
                    PostTextRun(text),
                ),
            )
        return ThreadPostUi(
            id,
            document,
            RichPostTextUi.create(document),
            ThreadPostMetadataUi(
                null,
                null,
                false,
                ThreadPostDisplay(index = id.toInt(), header = AnnotatedString("#$id 06.10 05:00:00")),
            ),
            emptyList(),
        )
    }
}
