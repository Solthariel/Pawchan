package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComposerImportAppearanceRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun importKeepsActionPixelsButBlocksBothClicksInBothThemes() {
        val state = mutableStateOf(screen(ComposerDraftActivity.IDLE))
        val scheme = mutableStateOf(darkColorScheme())
        var clicks = 0
        compose.setContent {
            MaterialTheme(colorScheme = scheme.value) {
                Box(Modifier.width(320.dp)) { ComposerActionPanel(state.value, false, { clicks++ }, { clicks++ }) }
            }
        }
        for (theme in listOf(darkColorScheme(), lightColorScheme())) {
            compose.runOnIdle { scheme.value = theme }
            val send = compose.onNodeWithTag("composer-send-action")
            val media = compose.onNodeWithContentDescription("Добавить медиа")
            val capsule = compose.onNodeWithTag("composer-action-capsule")
            send.assertIsEnabled()
            val before = capsule.captureToImage().toPixelMap()
            compose.runOnIdle { state.value = screen(ComposerDraftActivity.LOADING_FILES) }
            send.assertIsNotEnabled().performTouchInput { click() }
            media.assertIsNotEnabled().performTouchInput { click() }
            val during = capsule.captureToImage().toPixelMap()
            for (y in 0 until before.height) {
                for (x in 0 until before.width) assertEquals("$x,$y", before[x, y], during[x, y])
            }
            assertEquals(0, clicks)
            compose.runOnIdle { state.value = screen(ComposerDraftActivity.IDLE) }
            send.assertIsEnabled()
        }
    }

    private fun screen(activity: ComposerDraftActivity) =
        ComposerScreenState(
            ComposerDraftState(comment = "test", activity = activity),
            PostingRules(),
            ComposerCaptchaState(),
            ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), activity == ComposerDraftActivity.IDLE),
        )
}
