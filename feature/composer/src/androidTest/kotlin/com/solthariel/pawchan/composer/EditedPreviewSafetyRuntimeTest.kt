package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test

class EditedPreviewSafetyRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun restoredRecipeDoesNotShowOriginalWhilePreviewIsUnavailable() {
        val file =
            ComposerAttachment(
                source = ReplyAttachment("private.png", "image/png", byteArrayOf(1, 2, 3)),
                options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1))),
            )
        compose.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val loader = remember(context) { ImageLoader.Builder(context).build() }
                DisposableEffect(loader) { onDispose { loader.shutdown() } }
                Box(Modifier.size(200.dp)) {
                    ComposerAttachmentPreview(file, loader, Modifier.size(200.dp))
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Предпросмотр недоступен").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Предпросмотр недоступен").assertIsDisplayed()
        compose.onNodeWithContentDescription("private.png").assertDoesNotExist()
    }

    @Test
    fun loadingIsNotReportedAsFailureAndNoOriginalLeaks() {
        val result = CompletableDeferred<android.graphics.Bitmap?>()
        val render: suspend (ByteArray, DraftImageEdit) -> android.graphics.Bitmap? = { _, _ -> result.await() }
        val source = byteArrayOf(1, 2, 3)
        val edit = DraftImageEdit(quarterTurns = 1)
        compose.setContent {
            MaterialTheme {
                val state = rememberImageEditPreview(source, edit, render)
                ComposerEditedPreviewContent(state, "private.png", Modifier.size(200.dp))
            }
        }
        compose.onNodeWithText("Предпросмотр недоступен").assertDoesNotExist()
        compose.onNodeWithContentDescription("private.png").assertDoesNotExist()
        result.complete(null)
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Предпросмотр недоступен").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Предпросмотр недоступен").assertIsDisplayed()
        compose.onNodeWithContentDescription("private.png").assertDoesNotExist()
    }

    @Test
    fun changingRecipeHidesOldPreviewUntilNewResultIsReady() {
        val first = CompletableDeferred<android.graphics.Bitmap?>()
        val second = CompletableDeferred<android.graphics.Bitmap?>()
        val edit = mutableStateOf(DraftImageEdit(quarterTurns = 1))
        val render: suspend (ByteArray, DraftImageEdit) -> android.graphics.Bitmap? = { _, recipe ->
            if (recipe.quarterTurns == 1) first.await() else second.await()
        }
        val source = byteArrayOf(1, 2, 3)
        compose.setContent {
            MaterialTheme {
                val state = rememberImageEditPreview(source, edit.value, render)
                ComposerEditedPreviewContent(state, "private.png", Modifier.size(200.dp))
            }
        }
        first.complete(android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("private.png").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { edit.value = DraftImageEdit(quarterTurns = 2) }
        compose.onNodeWithContentDescription("private.png").assertDoesNotExist()
        compose.onNodeWithText("Предпросмотр недоступен").assertDoesNotExist()
        second.complete(android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888))
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("private.png").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("private.png").assertIsDisplayed()
    }
}
