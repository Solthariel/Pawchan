package com.solthariel.pawchan

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.core.ui.FastScrollGesture
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadAttachmentActions
import com.solthariel.pawchan.thread.ThreadPostCard
import com.solthariel.pawchan.thread.ThreadPostDisplay
import com.solthariel.pawchan.thread.ThreadPostInteractions
import com.solthariel.pawchan.thread.ThreadPostMetadataUi
import com.solthariel.pawchan.thread.ThreadPostOptions
import com.solthariel.pawchan.thread.ThreadPostUi
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostLongPressUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longPressInvokesPostMenuWithoutSelectingText() {
        var menus = 0
        var selections = 0
        val document = PostDocument.plain("Проверочный пост")
        val post =
            ThreadPostUi(
                1,
                document,
                RichPostTextUi.create(document),
                ThreadPostMetadataUi(
                    null,
                    null,
                    false,
                    ThreadPostDisplay(index = 1, header = AnnotatedString("#1")),
                ),
                emptyList(),
            )
        compose.setContent {
            val context = LocalContext.current
            val loader = remember { ThumbnailLoader(context, OkHttpClient()) }
            DisposableEffect(loader) { onDispose { loader.close() } }
            ThreadTestTheme(com.solthariel.pawchan.domain.settings.ThemePreference.DARK) {
                ThreadPostCard(
                    post,
                    ThumbnailLoading(loader, FastScrollGesture()),
                    ThreadAttachmentActions({}, {}, {}),
                    {},
                    ThreadPostOptions(
                        selected = true,
                        interactions =
                            ThreadPostInteractions(
                                onMenu = { menus++ },
                                onSelect = { selections++ },
                            ),
                    ),
                )
            }
        }
        compose.onNodeWithText("Проверочный пост").performTouchInput {
            down(center)
            advanceEventTime(700)
            up()
        }
        assertEquals(1, menus)
        compose.onNodeWithText("#1").performTouchInput { click(center) }
        compose.onNodeWithText("#1").performTouchInput {
            down(center)
            advanceEventTime(700)
            up()
        }
        assertEquals(2, menus)
        assertEquals(0, selections)
        compose.onNodeWithText("✓").assertDoesNotExist()
        compose.onNode(isSelected()).assertIsSelected()
        saveThreadChromeScreenshot("fix12-selected-post")
    }
}
