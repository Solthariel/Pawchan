package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttachmentThemeBackgroundRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun headerCanvasAndControlsUseMainThemeBackground() {
        val background = Color(0xFFF2E5D1)
        val file = ComposerAttachment(ReplyAttachment("broken.png", "image/png", byteArrayOf(1, 2, 3)))
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = background, surfaceContainer = Color.Green)) {
                Column {
                    AttachmentViewerHeader(true, {}, {}, draft)
                    Box(Modifier.size(320.dp, 120.dp)) {
                        ImageEditorCanvas(
                            byteArrayOf(1, 2, 3),
                            ImageEditorState(DraftImageEdit()),
                            ImageEditorCanvasConfig(ImageEditorCanvasMode.FILE),
                            {},
                        )
                    }
                    AttachmentEditorPanel(file, draft, null, true, Modifier.size(320.dp, 180.dp))
                }
            }
        }
        listOf("attachment-header", "attachment-image-canvas", "attachment-panel").forEach { tag ->
            val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            assertEquals("Background of $tag", background, pixels[0, 0])
        }
    }
}
