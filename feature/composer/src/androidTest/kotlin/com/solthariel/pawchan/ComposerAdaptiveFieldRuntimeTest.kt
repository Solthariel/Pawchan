package com.solthariel.pawchan

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerEditorScreen
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

/** «Поле ввода высоко»: текст с панелью занимает свободную высоту, вложения стоят над отправкой. */
class ComposerAdaptiveFieldRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var view: View

    @Test
    fun fieldFillsTheScreenAndToolbarStaysGluedUnderIt() {
        show(attachments = 0, height = 700)
        val field = bounds(field = true)
        val toolbar = compose.onNodeWithTag("composer-formatting-viewport").fetchSemanticsNode().boundsInRoot
        val capsule = compose.onNodeWithTag("composer-action-capsule").fetchSemanticsNode().boundsInRoot
        assertTrue("field ${field.height}", field.height > toolbar.height * 4)
        assertTrue(toolbar.top >= field.bottom && toolbar.top - field.bottom < 48f * density())
        assertTrue(toolbar.bottom <= capsule.top)
    }

    @Test
    fun mediaSitsAboveSendAndTheFieldShrinksByItsHeight() {
        var attachments by mutableIntStateOf(0)
        show(height = 700) { attachments }
        val empty = bounds(field = true).height
        compose.runOnIdle { attachments = 4 }
        compose.waitForIdle()
        val field = bounds(field = true)
        val media = compose.onNodeWithTag("composer-media-dock").fetchSemanticsNode().boundsInRoot
        val capsule = compose.onNodeWithTag("composer-action-capsule").fetchSemanticsNode().boundsInRoot
        assertTrue("field ${field.height} < $empty", field.height < empty)
        assertTrue(media.top >= field.bottom && media.bottom <= capsule.top + 1f)
    }

    @Test
    fun shortWindowKeepsTheMinimumFieldHeight() {
        show(attachments = 0, height = 300)
        // The field keeps its own 120dp and the area scrolls; the visible part may be clipped by the viewport.
        val field =
            compose
                .onNode(hasSetTextAction())
                .fetchSemanticsNode()
                .size.height
        assertTrue("field $field", field >= 120f * density() - 1f)
        compose.onNode(hasScrollAction() and hasAnyDescendant(hasSetTextAction())).assertExists()
    }

    private fun show(
        attachments: Int,
        height: Int,
    ) = show(height) { attachments }

    private fun show(
        height: Int,
        attachments: () -> Int,
    ) {
        compose.setContent {
            view = LocalView.current
            val count = attachments()
            val state = remember(count) { screenState(count) }
            MaterialTheme {
                val loader = ImageLoader(LocalContext.current).let { remember { it } }
                Box(Modifier.size(360.dp, height.dp)) {
                    ComposerEditorScreen(
                        "Новое сообщение",
                        false,
                        state,
                        loader,
                        ComposerScreenActions(
                            ComposerFieldActions({}, {}),
                            NewThreadCaptchaActions({}, {}),
                            {},
                            {},
                        ),
                    )
                }
            }
        }
        compose.waitForIdle()
        // A keyboard left open by an earlier test pads only the bottom of this short box, so measure without it.
        Espresso.closeSoftKeyboard()
        compose.waitUntil(KEYBOARD_TIMEOUT_MS) {
            ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        compose.waitForIdle()
    }

    private fun bounds(field: Boolean): Rect =
        if (field) compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot else Rect.Zero

    private fun density(): Float = compose.density.density
}

private fun screenState(attachments: Int): ComposerScreenState {
    val files =
        List(attachments) {
            ComposerAttachment(ReplyAttachment("$it.png", "image/png", byteArrayOf(1)), preview = null)
        }
    return ComposerScreenState(
        ComposerDraftState(attachments = files),
        PostingRules(),
        ComposerCaptchaState(),
        ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), false),
    )
}

private const val KEYBOARD_TIMEOUT_MS = 5_000L
