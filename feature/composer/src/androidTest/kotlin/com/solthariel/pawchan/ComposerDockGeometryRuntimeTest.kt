package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerAttachmentPickerState
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerEditorDock
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenActions
import com.solthariel.pawchan.composer.ComposerScreenState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerDockGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortWindowKeepsTheCapsuleVisibleAndLeavesRoomForTheForm() {
        val files = List(4) { ComposerAttachment(ReplyAttachment("$it.png", "image/png", byteArrayOf(1)), preview = null) }
        val state =
            ComposerScreenState(
                ComposerDraftState(attachments = files),
                PostingRules(),
                ComposerCaptchaState(),
                ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
            )
        compose.setContent {
            MaterialTheme {
                Column(Modifier.size(320.dp, 180.dp).testTag("editor")) {
                    Box(Modifier.fillMaxWidth().height(64.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).testTag("form"))
                    ComposerEditorDock(
                        state,
                        false,
                        {},
                        {},
                        Modifier,
                    )
                }
            }
        }
        val capsule =
            compose
                .onNodeWithTag("composer-action-capsule")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val editor = compose.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot
        val form = compose.onNodeWithTag("form").fetchSemanticsNode().boundsInRoot
        assertTrue(capsule.bottom <= editor.bottom)
        assertTrue(form.height > 0f && form.bottom <= capsule.top)
    }
}
