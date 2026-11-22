package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AttachmentEditorTransitionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolControlsDoNotMoveAfterTheirFirstFrameInEitherDirection() {
        val file = ComposerAttachment(ReplyAttachment("source.png", "image/png", byteArrayOf(1)))
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                AttachmentEditorPanel(file, draft, ReencodingDimensions(800, 600), true, Modifier.size(360.dp, 184.dp))
            }
        }
        compose.mainClock.autoAdvance = false
        val modes =
            listOf(
                AttachmentEditorTab.CROP to "Свободно",
                AttachmentEditorTab.DRAW to "Перо",
                AttachmentEditorTab.ROTATE to "Повернуть влево на 90°",
                AttachmentEditorTab.REDACT to "Заливка",
                AttachmentEditorTab.CROP to "Свободно",
            )
        modes.forEach { (tab, label) ->
            compose.runOnIdle { draft.selectTab(tab) }
            compose.mainClock.advanceTimeByFrame()
            val initial = bounds(label)
            compose.mainClock.advanceTimeBy(500)
            assertEquals("$tab controls moved after the first frame", initial, bounds(label))
        }
    }

    @Test fun constrainedControlsRemainScrollableAndResetToTheBottomOnSwitch() {
        val file = ComposerAttachment(ReplyAttachment("source.png", "image/png", byteArrayOf(1)))
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                AttachmentEditorPanel(file, draft, ReencodingDimensions(800, 600), true, Modifier.size(320.dp, 140.dp))
            }
        }
        compose.runOnIdle { draft.selectTab(AttachmentEditorTab.DRAW) }
        compose.onNodeWithContentDescription("Перо").assertIsDisplayed()
        compose.onNodeWithTag("attachment-editor-settings").performTouchInput { swipeDown() }
        compose.onNodeWithContentDescription("Выбрать цвет").assertIsDisplayed()
        compose.runOnIdle { draft.selectTab(AttachmentEditorTab.ROTATE) }
        compose.runOnIdle { draft.selectTab(AttachmentEditorTab.DRAW) }
        compose.onNodeWithContentDescription("Перо").assertIsDisplayed()
    }

    private fun bounds(label: String): Rect = compose.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot
}
