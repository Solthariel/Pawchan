package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttachmentEditorSpacingRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contextualButtonsStayFullyVisibleWithTheSameGapInEveryMode() {
        val file = ComposerAttachment(ReplyAttachment("source.png", "image/png", byteArrayOf(1)))
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                AttachmentEditorPanel(file, draft, ReencodingDimensions(800, 600), true, Modifier.size(360.dp, 184.dp))
            }
        }
        val modes =
            listOf(
                AttachmentEditorTab.CROP to "Свободно",
                AttachmentEditorTab.ROTATE to "Повернуть влево на 90°",
                AttachmentEditorTab.DRAW to "Перо",
                AttachmentEditorTab.REDACT to "Заливка",
                AttachmentEditorTab.CROP to "Свободно",
            )
        val density =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        modes.forEach { (tab, label) ->
            compose.runOnIdle { draft.selectTab(tab) }
            val choice =
                compose
                    .onNodeWithContentDescription(label)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            val actions = compose.onNodeWithContentDescription("Назад к файлу").fetchSemanticsNode().boundsInRoot
            assertEquals("$tab button height", 48f * density, choice.height, 1f)
            assertEquals("$tab spacing", 9f * density, actions.top - choice.bottom, 1.5f)
        }
    }
}
