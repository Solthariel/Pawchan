package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class AttachmentEditorConnectedRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun rotationIsExportedBeforeBackAppliesTheAttachment() {
        var saved: ComposerAttachment? = null
        val bitmap = Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888)
        val bytes =
            try {
                ByteArrayOutputStream().use { out ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                    out.toByteArray()
                }
            } finally {
                bitmap.recycle()
            }
        val file = ComposerAttachment(ReplyAttachment("image.png", "image/png", bytes))
        compose.setContent {
            MaterialTheme {
                val context = LocalContext.current
                val loader = remember(context) { ImageLoader.Builder(context).build() }
                DisposableEffect(loader) { onDispose { loader.shutdown() } }
                ComposerAttachmentViewer(file, loader, true, {}, { saved = it }, {})
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Изменить").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("editor-image-frame").fetchSemanticsNodes().isNotEmpty() }
        val before = compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Изменить").performClick()
        val panelBefore = compose.onNodeWithTag("attachment-panel").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("editor-tool-tabs").assertExists()
        compose.onNodeWithText("Текст").assertDoesNotExist()
        val after = compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(before, after)
        compose.onNodeWithContentDescription("Поворот").performClick()
        org.junit.Assert.assertEquals(panelBefore, compose.onNodeWithTag("attachment-panel").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Повернуть влево на 90°").performClick()
        compose.onNodeWithText("Готово").assertDoesNotExist()
        compose.onNodeWithContentDescription("Назад").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { saved != null }
        val prepared = requireNotNull(saved?.preparedUpload)
        org.junit.Assert.assertFalse(prepared.content.contentEquals(bytes))
        val result = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(prepared.content, 0, prepared.content.size))
        try {
            org.junit.Assert.assertEquals(30, result.width)
            org.junit.Assert.assertEquals(20, result.height)
        } finally {
            result.recycle()
        }
    }
}
