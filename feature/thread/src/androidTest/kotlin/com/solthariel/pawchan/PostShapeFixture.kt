package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.thread.FreshPostHighlight
import com.solthariel.pawchan.thread.HiddenPostCard
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAttachmentActions
import com.solthariel.pawchan.thread.ThreadPostCard
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostOptions
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.readingReturnMarker
import okhttp3.OkHttpClient

internal class PostShapeFixture(
    private val compose: ComposeContentTestRule,
) {
    fun show(
        bubble: MutableState<Boolean>,
        fresh: MutableState<Boolean> = androidx.compose.runtime.mutableStateOf(false),
        selected: Boolean = false,
        hidden: Boolean = false,
        marker: Boolean = false,
    ) {
        val document = PostDocument.plain("Текст поста")
        val post =
            ThreadPostUi(
                1,
                document,
                RichPostTextUi.create(document),
                ThreadPostMetadataUi(null, null, false, ThreadPostDisplay(index = 1, header = AnnotatedString("#1"))),
                emptyList(),
            )
        compose.setContent {
            val context = LocalContext.current
            val loader = remember { ThumbnailLoader(context, OkHttpClient()) }
            DisposableEffect(loader) { onDispose { loader.close() } }
            ThreadTestTheme(ThemePreference.DARK) {
                CompositionLocalProvider(LocalPostDisplayPreferences provides PostDisplayPreferences(bubblePosts = bubble.value)) {
                    Box(Modifier.background(Color.Magenta).padding(4.dp)) {
                        Box(Modifier.width(240.dp).testTag("post-shape").readingReturnMarker(marker)) {
                            if (hidden) {
                                HiddenPostCard(post, {}, {})
                            } else {
                                ThreadPostCard(
                                    post,
                                    ThumbnailLoading(loader, FastScrollGesture()),
                                    ThreadAttachmentActions({}, {}, {}),
                                    {},
                                    ThreadPostOptions(selected = selected),
                                )
                            }
                            FreshPostHighlight(fresh.value)
                        }
                    }
                }
            }
        }
    }

    fun pixels(): PixelMap = compose.onNodeWithTag("post-shape").captureToImage().toPixelMap()
}
