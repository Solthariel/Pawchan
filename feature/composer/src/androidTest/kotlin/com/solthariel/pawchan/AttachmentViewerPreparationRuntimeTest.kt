package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.solthariel.pawchan.composer.AttachmentViewerSource
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ImageEditorPreviewFrame
import com.solthariel.pawchan.composer.LocalAttachmentViewerSource
import com.solthariel.pawchan.composer.PreparedAttachmentViewer
import com.solthariel.pawchan.composer.ReencodingDimensions
import com.solthariel.pawchan.composer.rememberAttachmentEditorDimensions
import com.solthariel.pawchan.composer.rememberEditorPreview
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AttachmentViewerPreparationRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val file = ComposerAttachment(ReplyAttachment("test.png", "image/png", byteArrayOf(1)))

    @Test fun viewerIsNotPublishedUntilItsInitialSourceIsReady() {
        val ready = CompletableDeferred<AttachmentViewerSource>()
        compose.setContent {
            MaterialTheme {
                PreparedAttachmentViewer(file, {}, prepare = { ready.await() }) {
                    Box(Modifier.testTag("prepared-viewer"))
                }
            }
        }
        compose.onNodeWithTag("prepared-viewer").assertDoesNotExist()
        ready.complete(AttachmentViewerSource(file.source.content, null))
        compose.waitForIdle()
        compose.onNodeWithTag("prepared-viewer").assertExists()
    }

    @Test fun closingBeforePreparationCompletesCancelsWorkAndNeverOpensViewer() {
        val visible = mutableStateOf(true)
        val cancelled = AtomicBoolean(false)
        compose.setContent {
            if (visible.value) {
                PreparedAttachmentViewer(file, {}, prepare = {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled.set(true)
                    }
                }) { Box(Modifier.testTag("prepared-viewer")) }
            }
        }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        assertTrue(cancelled.get())
        compose.onNodeWithTag("prepared-viewer").assertDoesNotExist()
    }

    @Test fun failedPreparedEditIsPublishedAsFailureWithoutRetryingOrShowingAnOriginal() {
        val edit = DraftImageEdit(quarterTurns = 1)
        val failed = ImageEditorPreviewFrame(edit, null)
        val source = AttachmentViewerSource(file.source.content, ReencodingDimensions(4, 4))
        source.frames.put(edit, failed)
        compose.setContent {
            CompositionLocalProvider(LocalAttachmentViewerSource provides source) {
                assertSame(failed, rememberEditorPreview(file.source.content, edit))
            }
        }
        compose.waitForIdle()
    }

    @Test fun preparedDimensionsAndEditedPixelsAreAvailableInTheFirstComposition() {
        val edit = DraftImageEdit(quarterTurns = 1)
        val frame = ImageEditorPreviewFrame(edit, Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888))
        val dimensions = ReencodingDimensions(4, 4)
        val source = AttachmentViewerSource(file.source.content, dimensions)
        source.frames.put(edit, frame)
        compose.setContent {
            CompositionLocalProvider(LocalAttachmentViewerSource provides source) {
                assertEquals(dimensions, rememberAttachmentEditorDimensions(file))
                assertSame(frame, rememberEditorPreview(file.source.content, edit))
            }
        }
        compose.waitForIdle()
    }
}
