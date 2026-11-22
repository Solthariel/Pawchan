package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class AttachmentPanelGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun croppedImageAndPanelDoNotRefitWhenToolsChange() {
        val options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(crop = DraftCrop(.1f, .2f, .6f, .9f))))
        val file =
            ComposerAttachment(ReplyAttachment("image.png", "image/png", samplePng()), options = options)
        val draft = AttachmentViewerDraft(options)
        compose.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val loader = remember(context) { ImageLoader.Builder(context).build() }
                DisposableEffect(loader) { onDispose { loader.shutdown() } }
                Box(Modifier.size(320.dp, 640.dp)) { AttachmentEditorContent(file, loader, draft, true) }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("editor-image-frame").fetchSemanticsNodes().isNotEmpty() }
        val frame = compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("attachment-panel").fetchSemanticsNode().boundsInRoot
        AttachmentEditorTab.entries.forEach { tab ->
            compose.runOnIdle { draft.selectTab(tab) }
            compose.waitForIdle()
            assertEquals(frame, compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot)
            assertEquals(panel, compose.onNodeWithTag("attachment-panel").fetchSemanticsNode().boundsInRoot)
        }
        compose.runOnIdle { draft.selectTab(AttachmentEditorTab.REDACT) }
        val tool =
            compose
                .onNodeWithContentDescription("Замазать")
                .fetchSemanticsNode()
                .boundsInRoot
        val density =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        assertTrue(tool.width >= 48 * density && tool.height >= 48 * density)
    }

    @Test fun allEditorModesKeepANineDpGapAboveTheActionRow() {
        val file = ComposerAttachment(ReplyAttachment("image.png", "image/png", samplePng()))
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val loader = remember(context) { ImageLoader.Builder(context).build() }
                DisposableEffect(loader) { onDispose { loader.shutdown() } }
                Box(Modifier.size(320.dp, 640.dp)) { AttachmentEditorContent(file, loader, draft, true) }
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("editor-image-frame").fetchSemanticsNodes().isNotEmpty() }

        val modes =
            listOf(
                AttachmentEditorTab.CROP to "4:3",
                AttachmentEditorTab.ROTATE to "Повернуть вправо на 90°",
                AttachmentEditorTab.DRAW to "Перо",
                AttachmentEditorTab.REDACT to "Заливка",
            )
        val density =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        modes.forEach { (tab, controlLabel) ->
            compose.runOnIdle { draft.selectTab(tab) }
            compose.waitForIdle()
            val control = compose.onNodeWithContentDescription(controlLabel).fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithContentDescription("Назад к файлу").fetchSemanticsNode().boundsInRoot
            assertEquals("$tab gap", 9f * density, action.top - control.bottom, 1f)
        }
    }

    @Test fun undecodableStillImageShowsDisabledEditInsteadOfHidingIt() {
        val bytes = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        val file = ComposerAttachment(ReplyAttachment("broken.png", "image/png", bytes))
        compose.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val loader = remember(context) { ImageLoader.Builder(context).build() }
                DisposableEffect(loader) { onDispose { loader.shutdown() } }
                Box(
                    Modifier.size(320.dp, 640.dp),
                ) { AttachmentEditorContent(file, loader, remember { AttachmentViewerDraft(file.options) }, true) }
            }
        }
        compose.onNodeWithContentDescription("Изменить").assertIsNotEnabled()
    }

    @Test fun comparisonTogglesWithoutChangingTheDraft() {
        val options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1)))
        val draft = AttachmentViewerDraft(options)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 184.dp)) {
                    AttachmentEditorTabs(draft, true)
                }
            }
        }
        compose.onNodeWithContentDescription("Сравнить").performClick()
        compose.runOnIdle {
            assertTrue(draft.comparing)
            assertEquals(options, draft.currentOptions)
        }
        compose.onNodeWithContentDescription("Сравнить").performClick()
        compose.runOnIdle {
            assertTrue(!draft.comparing)
            assertEquals(options, draft.currentOptions)
        }
    }

    @Test fun comparisonImageUsesTheSameFittedFrameAsTheEditor() {
        val original = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
        val source = samplePng()
        val state = ImageEditorState(DraftImageEdit())
        compose.setContent {
            MaterialTheme {
                DisposableEffect(original) {
                    onDispose { original.recycle() }
                }
                Box(Modifier.size(320.dp, 640.dp)) {
                    ImageEditorCanvas(
                        source,
                        state,
                        ImageEditorCanvasConfig(ImageEditorCanvasMode.FILE),
                        {},
                        options = ImageCanvasOptions(comparisonBitmap = original),
                    )
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("editor-comparison-image").fetchSemanticsNodes().isNotEmpty()
        }
        val imageFrame = compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot
        val comparison = compose.onNodeWithTag("editor-comparison-image").fetchSemanticsNode().boundsInRoot
        assertEquals(imageFrame, comparison)
    }
}

private fun samplePng(): ByteArray {
    val image = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
    return try {
        ByteArrayOutputStream().use { stream ->
            check(image.compress(Bitmap.CompressFormat.PNG, 100, stream))
            stream.toByteArray()
        }
    } finally {
        image.recycle()
    }
}
