package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostRating
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadPostByline
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostHeader
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostUi
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostMaterialHeaderRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private var density = 1f

    @Test fun identityAndDateStayWithinNarrowHeaderAtLargeFont() {
        show(2f)
        val parent = compose.onNodeWithTag("post-header").fetchSemanticsNode().boundsInRoot
        for (label in listOf("125", "#2765041", "30.10.26 19:04:00")) {
            val bounds = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= parent.left && bounds.right <= parent.right + 1f)
            assertTrue(bounds.top >= parent.top && bounds.bottom <= parent.bottom + 1f)
        }
    }

    @Test fun ratingsReserveSeparate48dpTargetsAndRemainDisabledWithoutSession() {
        show(1f)
        val like =
            compose
                .onNodeWithContentDescription("Нравится: 17")
                .assertIsNotEnabled()
                .fetchSemanticsNode()
                .boundsInRoot
        val dislike =
            compose
                .onNodeWithContentDescription("Не нравится: 3")
                .assertIsNotEnabled()
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(like.width >= 48f * density - 1f && like.height >= 48f * density - 1f)
        assertTrue(dislike.width >= 48f * density - 1f && dislike.height >= 48f * density - 1f)
        assertTrue(dislike.left >= like.right)
    }

    private fun show(scale: Float) {
        val document = PostDocument.plain("Текст")
        val post =
            ThreadPostUi(
                2765041,
                document,
                RichPostTextUi.create(document),
                ThreadPostMetadataUi(
                    null,
                    null,
                    false,
                    ThreadPostDisplay(
                        index = 125,
                        header = AnnotatedString("#2765041"),
                        byline = ThreadPostByline(author = "Автор", date = "30.10.26 19:04:00"),
                    ),
                    rating = PostRating(17, 3),
                ),
                emptyList(),
            )
        compose.setContent {
            val context = LocalContext.current
            density = LocalDensity.current.density
            val loader = remember { ThumbnailLoader(context, OkHttpClient()) }
            DisposableEffect(loader) { onDispose { loader.close() } }
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                MaterialTheme {
                    Box(Modifier.width(240.dp).testTag("post-header")) {
                        ThreadPostHeader(post, ThumbnailLoading(loader, FastScrollGesture()))
                    }
                }
            }
        }
    }
}
