package com.solthariel.pawchan

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.core.ui.NO_POST_COLLAPSE
import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAppearance
import com.solthariel.pawchan.thread.ThreadAttachmentActions
import com.solthariel.pawchan.thread.ThreadPostCard
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostOptions
import com.solthariel.pawchan.thread.ThreadPostUi
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostFooterRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun expandStandsNextToRepliesAndTakesTheRest() {
        showPost(long = true, replies = "2 ответа")
        val replies =
            compose
                .onNodeWithText("2 ответа")
                .fetchSemanticsNode()
                .boundsInRoot.width
        val expand =
            compose
                .onNodeWithText("Раскрыть")
                .fetchSemanticsNode()
                .boundsInRoot.width
        // «Раскрыть» стоит сразу за ответами и занимает остаток строки.
        assertTrue(expand > replies)
        compose.onNodeWithText("Раскрыть").performClick()
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
        assertTrue(
            compose
                .onNodeWithText("2 ответа")
                .fetchSemanticsNode()
                .boundsInRoot.width > replies,
        )
    }

    @Test
    fun loneExpandUsesFullWidthAndActuallyRevealsText() {
        showPost(long = true, replies = null)
        val width =
            compose
                .onNodeWithText("Раскрыть")
                .fetchSemanticsNode()
                .boundsInRoot.width
        assertTrue(
            width > compose
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width * 0.9f,
        )
        compose.onNodeWithText("Раскрыть").performClick()
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }

    @Test
    fun loneRepliesUseFullWidth() {
        showPost(long = false, replies = "Ответов: 1")
        val width =
            compose
                .onNodeWithText("Ответов: 1")
                .fetchSemanticsNode()
                .boundsInRoot.width
        assertTrue(
            width > compose
                .onRoot()
                .fetchSemanticsNode()
                .boundsInRoot.width * 0.9f,
        )
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }

    @Test
    fun longReplyByOriginalPosterIsNotCollapsed() {
        showPost(long = true, replies = null, opening = false)
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }

    @Test
    fun noCollapseSettingKeepsLongOpeningPostExpanded() {
        showPost(long = true, replies = null, maxLines = NO_POST_COLLAPSE)
        compose.onNodeWithText("Раскрыть").assertDoesNotExist()
    }

    private fun showPost(
        long: Boolean,
        replies: String?,
        opening: Boolean = true,
        maxLines: Int = 2,
    ) {
        val document = PostDocument.plain(if (long) "Длинная строка текста поста.\n".repeat(40) else "Короткий пост")
        val post =
            ThreadPostUi(
                1,
                document,
                RichPostTextUi.create(document),
                ThreadPostMetadataUi(
                    null,
                    null,
                    false,
                    ThreadPostDisplay(
                        index = 1,
                        header = AnnotatedString("#1"),
                        markers = listOf(PostStateMarker.ORIGINAL_POSTER),
                    ),
                ),
                emptyList(),
                openingPost = opening,
            )
        compose.setContent {
            val context = LocalContext.current
            val loader = remember { ThumbnailLoader(context, OkHttpClient()) }
            DisposableEffect(loader) { onDispose { loader.close() } }
            ThreadTestTheme(ThemePreference.DARK) {
                ThreadPostCard(
                    post,
                    ThumbnailLoading(loader, FastScrollGesture()),
                    ThreadAttachmentActions({}, {}, {}),
                    {},
                    ThreadPostOptions(appearance = ThreadAppearance(maxLines = maxLines), replyLabel = replies),
                )
            }
        }
    }
}
