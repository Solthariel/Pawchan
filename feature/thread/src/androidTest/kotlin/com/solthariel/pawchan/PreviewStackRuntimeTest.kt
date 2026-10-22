package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.test.espresso.Espresso
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.thread.PostPreviewActions
import com.solthariel.pawchan.thread.PostPreviewBindings
import com.solthariel.pawchan.thread.PostPreviewResources
import com.solthariel.pawchan.thread.PostPreviewStack
import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAppearance
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.withPostNavigation
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PreviewStackRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun returningToRepliesPreservesScrollAndDoesNotShowLoading() {
        val targets = showStack(listOf(PostPreviewTarget(null, 1, replies = true)))
        compose
            .onNode(hasTestTag("preview-list") and hasAnyAncestor(hasTestTag("preview-active")), useUnmergedTree = true)
            .performScrollToNode(hasText("Пост 30", substring = true))
        val before = compose.onNodeWithText("Пост 30", substring = true).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { targets.value += PostPreviewTarget(null, 2) }
        compose.onNodeWithText("Пост 2", substring = true).assertIsDisplayed()
        Espresso.pressBack()
        compose.waitForIdle()
        assertEquals(before, compose.onNodeWithText("Пост 30", substring = true).fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("preview-loading").assertDoesNotExist()
    }

    @Test fun upwardSwipePopsOneAndDownwardSwipeClearsTheChain() {
        val targets = showStack(listOf(PostPreviewTarget(null, 2), PostPreviewTarget(null, 3)))
        compose.onNodeWithText("Пост 3", substring = true).assertIsDisplayed()
        pullWindow(upwards = true)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(PostPreviewTarget(null, 2)), targets.value) }
        pullWindow(upwards = false)
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(emptyList<PostPreviewTarget>(), targets.value) }
    }

    @Test fun scrollingRepliesDoesNotDismissAndDeepChainHasOneActiveWindow() {
        val targets = showStack(listOf(PostPreviewTarget(null, 1, replies = true)))
        compose
            .onNode(hasTestTag("preview-list") and hasAnyAncestor(hasTestTag("preview-active")), useUnmergedTree = true)
            .performScrollToNode(hasText("Пост 30", substring = true))
        compose.onNodeWithTag("preview-active").performTouchInput { swipeDown(durationMillis = 400) }
        compose.runOnIdle { assertEquals(1, targets.value.size) }
        compose.runOnIdle { targets.value = (2L..8L).map { PostPreviewTarget(null, it) } }
        compose.waitForIdle()
        compose.onAllNodes(hasTestTag("preview-active")).assertCountEquals(1)
        repeat(4) {
            Espresso.pressBack()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Пост 4", substring = true).assertIsDisplayed()
    }

    @Test fun singleShortPostDoesNotExpandToScreenHeight() {
        showStack(listOf(PostPreviewTarget(null, 2)))
        val window = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Short preview must wrap its content", window.height < screen.height / 2f)
        assertTrue(
            "Preview must remain inside the screen",
            screen.contains(window.topLeft) && screen.contains(window.bottomRight),
        )
    }

    @Test fun nestedShortPostShrinksOverLongRepliesAndBackRestoresTheirHeight() {
        val targets = showStack(listOf(PostPreviewTarget(null, 1, replies = true)))
        val replies = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { targets.value += PostPreviewTarget(null, 2) }
        compose.waitForIdle()
        val single = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        assertTrue("A short nested reply must not inherit the list height", single.height < replies.height / 2f)
        Espresso.pressBack()
        compose.waitForIdle()
        assertEquals(replies, compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun inactiveWindowDoesNotShiftOrShrinkWhenPushingAnotherReply() {
        val targets = showStack(listOf(PostPreviewTarget(null, 1, replies = true)))
        val before = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { targets.value += PostPreviewTarget(null, 2) }
        compose.waitForIdle()
        val behind = compose.onNodeWithTag("preview-behind", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(before, behind)
        compose.onNodeWithTag("preview-loading").assertDoesNotExist()
    }

    @Test fun liveChainSurvivesSourceRefreshAndBackWithoutEmptyWindows() {
        val local = mutableStateOf(content())
        val targets = showStack(listOf(PostPreviewTarget(null, 2)), local)
        compose.runOnIdle { targets.value += listOf(PostPreviewTarget(null, 3), PostPreviewTarget(null, 4)) }
        compose.onNodeWithText("Пост 4", substring = true).assertIsDisplayed()
        compose.runOnIdle {
            local.value = local.value.copy(posts = local.value.posts.take(1)).withPostNavigation()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Пост 4", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("preview-loading").assertDoesNotExist()
        repeat(2) {
            Espresso.pressBack()
            compose.waitForIdle()
            compose.onNodeWithText("Пост ${3 - it}", substring = true).assertIsDisplayed()
            compose.onNodeWithTag("preview-loading").assertDoesNotExist()
        }
    }

    private fun pullWindow(upwards: Boolean) {
        val window = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val start = window.center - root.topLeft
        val end = Offset(start.x, root.height * if (upwards) 0.05f else 0.95f)
        compose.onRoot().performTouchInput { swipe(start, end, durationMillis = 160) }
    }

    private fun showStack(
        initial: List<PostPreviewTarget>,
        local: MutableState<ThreadUiState.Content> = mutableStateOf(content()),
    ): MutableState<List<PostPreviewTarget>> {
        val targets = mutableStateOf(initial)
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                val resources = previewResources()
                if (targets.value.isNotEmpty()) {
                    PostPreviewStack(
                        targets.value,
                        local.value,
                        ThreadAppearance(),
                        resources,
                        bindings = { _ ->
                            PostPreviewBindings(
                                { targets.value = targets.value.dropLast(1) },
                                {},
                                {},
                                {},
                                {},
                            )
                        },
                        onClear = { targets.value = emptyList() },
                    )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose
                .onAllNodes(
                    hasTestTag("preview-list") and hasAnyAncestor(hasTestTag("preview-active")),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        return targets
    }

    @Composable
    private fun previewResources(): PostPreviewResources {
        val context = LocalContext.current
        val client = remember { OkHttpClient() }
        val thumbnails = remember { ThumbnailLoader(context, client) }
        DisposableEffect(thumbnails) {
            onDispose {
                thumbnails.close()
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
        val actions =
            remember {
                object : PostPreviewActions {
                    override fun prepareReply(
                        address: ThreadAddress,
                        text: String,
                    ) = Unit

                    override fun navigate(address: ThreadAddress) = Unit
                }
            }
        return PostPreviewResources(null, thumbnails, actions)
    }

    private fun content(): ThreadUiState.Content {
        val posts =
            (1L..41L).map { id ->
                val document =
                    PostDocument(
                        listOf(
                            PostTextRun(">>1\n", link = PostLink.Reference(requireNotNull(PostId.parse(1)))),
                            PostTextRun("Пост $id"),
                        ),
                    )
                ThreadPostUi(
                    id,
                    document,
                    RichPostTextUi.create(document),
                    ThreadPostMetadataUi(null, null, false),
                    emptyList(),
                )
            }
        return ThreadUiState
            .Content(
                1,
                null,
                posts,
                ThreadContentFlags(false),
                ThreadContentPresentation(false),
                ThreadContentStatistics(),
            ).withPostNavigation()
    }
}
