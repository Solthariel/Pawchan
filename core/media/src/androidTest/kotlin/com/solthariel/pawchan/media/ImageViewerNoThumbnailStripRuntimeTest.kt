package com.solthariel.pawchan.media

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import com.solthariel.pawchan.domain.model.MediaKind
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ImageViewerNoThumbnailStripRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val loader =
        ImageLoader
            .Builder(context)
            .components {
                add(
                    Interceptor { chain ->
                        ErrorResult(
                            image = null,
                            request = chain.request,
                            throwable = IOException("image fixture"),
                        )
                    },
                )
            }.build()

    @After fun closeLoader() = loader.shutdown()

    @Test
    fun groupedImagesDoNotShowThumbnailStrip() {
        val items = listOf(attachment("first"), attachment("second"))
        val selected = items.first()
        val navigation = MediaViewerSelection(selected, items) {}.navigation()
        compose.setContent {
            MaterialTheme {
                ImageViewer(selected, loader, {}, null, {}, navigation)
            }
        }
        compose.onNodeWithTag("media-group-thumbnails").assertDoesNotExist()
    }

    private fun attachment(id: String): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(
                id = id,
                fileName = "$id.jpg",
                originalUrl = "https://fixture.test/$id.jpg",
                thumbnailUrl = "https://fixture.test/$id-thumb.jpg",
            ),
            byteSize = null,
            dimensions = null,
            viewer = AttachmentViewer.IMAGE,
            kind = MediaKind.IMAGE,
        )
}
