package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.ComposerActionPanel
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftActivity
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerActionPanelUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(screen())
    private var sends = 0
    private var attaches = 0
    private var mediaLabel = ""

    @Test fun disabledSendDoesNotDisableMediaAndClicksNeverSend() {
        show()
        compose.onNodeWithTag("composer-send-action").assertIsNotEnabled()
        compose.onNodeWithContentDescription(mediaLabel).assertIsEnabled().performTouchInput { click() }
        assertEquals(1, attaches)
        assertEquals(0, sends)
    }

    @Test fun actionsAreIndependentAndMediaIsInsideTheRightEnd() {
        state.value = screen(canSubmit = true)
        show()
        val capsule = compose.onNodeWithTag("composer-action-capsule")
        val media = compose.onNodeWithContentDescription(mediaLabel)
        val bounds = capsule.fetchSemanticsNode().boundsInRoot
        val mediaBounds = media.fetchSemanticsNode().boundsInRoot
        assertTrue(mediaBounds.left > bounds.center.x && mediaBounds.right <= bounds.right)
        assertTrue(mediaBounds.width >= 48 * compose.density.density)
        media.performTouchInput { click() }
        assertEquals(0, sends)
        capsule.performTouchInput {
            click(
                androidx.compose.ui.geometry
                    .Offset(width * 0.25f, height / 2f),
            )
        }
        assertEquals(1, sends)
        assertEquals(1, attaches)
    }

    @Test fun sendLabelIsCenteredInTheWholeCapsule() {
        state.value = screen(canSubmit = true)
        show()
        val capsule = compose.onNodeWithTag("composer-action-capsule").fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithText("Отправить", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(capsule.center.x, label.center.x, compose.density.density)
    }

    @Test fun enabledControlsFollowSurfaceColorsInBothThemes() {
        val dark =
            darkColorScheme(
                surfaceContainerHigh = Color(0xff314152),
                surfaceContainerHighest = Color(0xff536475),
            )
        val light =
            lightColorScheme(
                surfaceContainerHigh = Color(0xffd2e3f4),
                surfaceContainerHighest = Color(0xffa5b6c7),
            )
        val scheme = mutableStateOf(dark)
        state.value = screen(canSubmit = true)
        compose.setContent {
            MaterialTheme(colorScheme = scheme.value) {
                mediaLabel = stringResource(R.string.attachment_add)
                Box(Modifier.width(320.dp)) {
                    ComposerActionPanel(state.value, false, { attaches++ }, { sends++ })
                }
            }
        }
        for (next in listOf(dark, light)) {
            compose.runOnIdle { scheme.value = next }
            val capsule = compose.onNodeWithTag("composer-action-capsule").captureToImage().toPixelMap()
            val media = compose.onNodeWithContentDescription(mediaLabel).captureToImage().toPixelMap()
            assertColor(next.surfaceContainerHigh, capsule[capsule.width / 4, capsule.height / 2])
            assertColor(next.surfaceContainerHighest, media[media.width / 2, media.height / 8])
        }
    }

    private fun assertColor(
        expected: Color,
        actual: Color,
    ) {
        assertEquals(expected.red, actual.red, 0.01f)
        assertEquals(expected.green, actual.green, 0.01f)
        assertEquals(expected.blue, actual.blue, 0.01f)
    }

    @Test fun disabledMediaAtTheLimitNeverFallsThroughToSend() {
        val files =
            List(4) {
                com.solthariel.pawchan.composer.ComposerAttachment(
                    com.solthariel.pawchan.domain.posting
                        .ReplyAttachment("$it.png", "image/png", byteArrayOf(1)),
                    preview = null,
                )
            }
        state.value = screen(canSubmit = true).copy(draft = ComposerDraftState(attachments = files))
        show()
        compose.onNodeWithContentDescription(mediaLabel).assertIsNotEnabled().performTouchInput { click() }
        assertEquals(0, sends)
        assertEquals(0, attaches)
        compose.onNodeWithTag("composer-send-action").performTouchInput { click() }
        assertEquals(1, sends)
    }

    @Test fun largeTextKeepsTheMediaTargetSeparateAndFullyVisible() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current.density
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit
                        .Density(density, 2f),
            ) {
                MaterialTheme {
                    mediaLabel = stringResource(R.string.attachment_add)
                    Box(Modifier.width(240.dp)) {
                        ComposerActionPanel(state.value, false, { attaches++ }, { sends++ })
                    }
                }
            }
        }
        val capsule = compose.onNodeWithTag("composer-action-capsule").fetchSemanticsNode().boundsInRoot
        val media = compose.onNodeWithContentDescription(mediaLabel).fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText("Отправить", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(text.right <= media.left)
        assertTrue(media.top >= capsule.top && media.bottom <= capsule.bottom)
    }

    @Test fun restoringAndSendingBlockBothActions() {
        show()
        for (next in listOf(
            screen().copy(draft = ComposerDraftState(activity = ComposerDraftActivity.RESTORING)),
            screen().copy(status = ComposerScreenStatus(ComposerProgressState(submitting = true), ComposerFailureState(), false)),
        )) {
            compose.runOnIdle { state.value = next }
            compose.onNodeWithTag("composer-send-action").assertIsNotEnabled()
            compose.onNodeWithContentDescription(mediaLabel).assertIsNotEnabled()
        }
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                mediaLabel = stringResource(R.string.attachment_add)
                Box(Modifier.width(320.dp)) {
                    ComposerActionPanel(state.value, false, { attaches++ }, { sends++ })
                }
            }
        }
    }

    private fun screen(canSubmit: Boolean = false) =
        ComposerScreenState(
            ComposerDraftState(),
            PostingRules(),
            ComposerCaptchaState(),
            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), canSubmit),
        )
}
