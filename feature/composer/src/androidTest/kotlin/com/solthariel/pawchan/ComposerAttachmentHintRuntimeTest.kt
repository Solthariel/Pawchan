package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFieldActions
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.NewThreadActions
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.NewThreadScreen
import com.solthariel.pawchan.composer.NewThreadUiState
import com.solthariel.pawchan.domain.model.BoardId
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ComposerAttachmentHintRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noAttachmentHintIsShownButAThreadWithoutMediaCannotBeSubmitted() {
        val state =
            NewThreadUiState(
                draft = ComposerDraftState(comment = "Test comment"),
                captchaState = ComposerCaptchaState(satisfied = true),
                progress = ComposerProgressState(captchaLoading = false),
            )
        assertFalse(state.canSubmit)
        compose.setContent {
            val context = LocalContext.current
            val imageLoader = remember(context) { ImageLoader.Builder(context).build() }
            DisposableEffect(imageLoader) { onDispose { imageLoader.shutdown() } }
            MaterialTheme {
                NewThreadScreen(
                    requireNotNull(BoardId.parse("b")),
                    state,
                    imageLoader,
                    NewThreadActions({}, ComposerFieldActions({}, {}), NewThreadCaptchaActions({}, {}), {}),
                )
            }
        }
        compose.onNodeWithText("Для нового треда требуется изображение или видео.").assertDoesNotExist()
        compose.onNodeWithText("Создать тред").assertIsNotEnabled()
    }
}
