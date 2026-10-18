package com.solthariel.pawchan

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.GalleryFilter
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGallery
import com.solthariel.pawchan.media.MediaGallerySession
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.media.toMediaGalleryContent
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MediaGalleryReturnUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun openingMediaSuspendsWindowWithoutClosingOrResettingGallery() {
        val loader = ThumbnailLoader(InstrumentationRegistry.getInstrumentation().targetContext, OkHttpClient())
        val session = MediaGallerySession().apply { filter = GalleryFilter.IMAGES }
        val visible = mutableStateOf(true)
        var closed = 0
        var opened = 0
        val gallery =
            (0 until 48)
                .map { index ->
                    MediaAttachmentUi(
                        MediaAttachmentSource("image-$index", "image$index.png", "https://example.org/$index.png", null),
                        null,
                        AttachmentViewer.IMAGE,
                        MediaKind.IMAGE,
                    )
                }.toMediaGalleryContent()
        try {
            compose.setContent {
                MaterialTheme {
                    if (!visible.value) Button(onClick = { visible.value = true }) { Text("Вернуться из просмотра") }
                    MediaGallery(
                        gallery,
                        loader,
                        com.solthariel.pawchan.media.MediaGalleryActions(
                            {
                                closed++
                                visible.value = false
                            },
                            {
                                opened++
                                visible.value = false
                            },
                        ),
                        session,
                        visible.value,
                    )
                }
            }
            compose.onNode(hasScrollAction()).performScrollToIndex(5)
            val anchor = session.list.firstVisibleItemIndex to session.list.firstVisibleItemScrollOffset
            compose.onNode(hasScrollAction()).performTouchInput { click(Offset(40f, 40f)) }
            compose.runOnIdle {
                assertEquals(1, opened)
                assertEquals(0, closed)
                assertFalse(visible.value)
            }
            compose.onNodeWithText("Вернуться из просмотра").performClick()
            compose.onNodeWithText("Галерея").assertExists()
            compose.runOnIdle {
                assertEquals(GalleryFilter.IMAGES, session.filter)
                assertEquals(anchor, session.list.firstVisibleItemIndex to session.list.firstVisibleItemScrollOffset)
                assertTrue(visible.value)
            }
        } finally {
            compose.runOnIdle { loader.close() }
        }
    }
}
