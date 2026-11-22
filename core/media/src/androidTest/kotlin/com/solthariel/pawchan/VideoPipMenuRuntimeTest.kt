package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContextMenu
import com.solthariel.pawchan.media.MediaMenuActions
import com.solthariel.pawchan.media.MediaMenuPlace
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaViewerMenu
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoSurface
import com.solthariel.pawchan.media.video.VideoSurfaceActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoPipMenuRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pipMenuItemClosesMenuAndInvokesPip() {
        var calls = 0
        var closed = 0
        val request = MediaMenuRequest(video(), MediaMenuPlace.VIEWER, MediaViewerMenu(anchored = true, onPip = { calls++ }))
        compose.setContent {
            MaterialTheme {
                MediaContextMenu(request, actions(), onClose = { closed++ })
            }
        }
        compose.onNodeWithText("Картинка в картинке").performClick()
        compose.runOnIdle {
            assertEquals(1, closed)
            assertEquals(1, calls)
        }
    }

    @Test fun viewerMoreMenuCarriesTheActualPipAction() {
        var pipCalls = 0
        var menu: MediaViewerMenu? = null
        val player = posterTestSession(false)
        val loader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
        try {
            compose.setContent {
                MaterialTheme {
                    VideoSurface(
                        player = player,
                        video = video(),
                        state = VideoPlaybackStatus(),
                        actions = VideoSurfaceActions(loader, {}, {}, { menu = it }, {}, { pipCalls++ }),
                        navigation = MediaViewerNavigation(),
                    )
                }
            }
            compose.onNodeWithContentDescription("Меню файла").performClick()
            compose.runOnIdle {
                requireNotNull(menu?.onPip).invoke()
                assertEquals(1, pipCalls)
            }
        } finally {
            compose.runOnIdle { player.close() }
            loader.shutdown()
        }
    }

    @Test fun unavailablePipIsNotShown() {
        val request = MediaMenuRequest(video(), MediaMenuPlace.VIEWER, MediaViewerMenu(anchored = true))
        compose.setContent { MaterialTheme { MediaContextMenu(request, actions(), onClose = {}) } }
        compose.onNodeWithText("Картинка в картинке").assertDoesNotExist()
    }

    private fun video() =
        MediaAttachmentUi(
            MediaAttachmentSource("a", "a.mp4", "https://example.invalid/a.mp4", null),
            null,
            AttachmentViewer.VIDEO,
            MediaKind.VIDEO,
        )

    private fun actions() =
        MediaMenuActions(
            onDownload = {},
            onCopyLink = {},
            onCopyImage = null,
            onSearch = {},
            onFavorite = {},
        )
}
