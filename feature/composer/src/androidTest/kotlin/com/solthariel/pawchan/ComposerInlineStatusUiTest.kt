package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ComposerStatusLine
import com.solthariel.pawchan.composer.ReplyError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerInlineStatusUiTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var loader: coil3.ImageLoader

    @org.junit.Before fun createLoader() {
        loader =
            coil3.ImageLoader
                .Builder(
                    androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .targetContext,
                ).build()
    }

    @org.junit.After fun closeLoader() {
        loader.shutdown()
    }

    @Test
    fun longReasonWrapsWithoutTruncationAndKeepsReloadAtRight() {
        var reloaded = 0
        val failure = mutableStateOf(ComposerFailureState())
        val reason = ("Файл слишком большой: " + "подробная причина ".repeat(30)).trimEnd()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp).verticalScroll(rememberScrollState())) {
                    ComposerStatusLine(
                        ComposerCaptchaState(),
                        ComposerScreenStatus(ComposerProgressState(), failure.value, false),
                        { reloaded++ },
                        loader,
                    )
                }
            }
        }
        val reload = compose.onNodeWithContentDescription("Повторить загрузку капчи")
        val before = reload.fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { failure.value = ComposerFailureState(ReplyError.REJECTED, reason) }
        val status = compose.onNodeWithText(reason)
        status.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        status.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().lineCount > 1)
        for (line in 0 until layouts.single().lineCount) assertFalse(layouts.single().isLineEllipsized(line))
        assertFalse(status.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
        reload.assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, reloaded) }
        assertEquals(before.top, reload.fetchSemanticsNode().boundsInRoot.top)
        assertEquals(before.left, reload.fetchSemanticsNode().boundsInRoot.left)
        assertEquals(before.right, reload.fetchSemanticsNode().boundsInRoot.right)
        compose.onNodeWithText("Закрыть").assertDoesNotExist()
        compose.onNodeWithText("Ошибка — нажмите для подробностей").assertDoesNotExist()
        compose.runOnIdle { failure.value = ComposerFailureState() }
        reload.assertIsDisplayed()
        assertEquals(before, reload.fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun reloadStaysAvailableAfterFailureButNotDuringSendOrSuccess() {
        var reloaded = 0
        val progress = mutableStateOf(ComposerProgressState())
        compose.setContent {
            MaterialTheme {
                ComposerStatusLine(
                    ComposerCaptchaState(),
                    ComposerScreenStatus(progress.value, ComposerFailureState(ReplyError.NETWORK), false),
                    { reloaded++ },
                    loader,
                )
            }
        }
        compose.onNodeWithText("Нет соединения с 2ch").assertIsDisplayed()
        compose.onNodeWithContentDescription("Повторить загрузку капчи").performClick()
        assertEquals(1, reloaded)
        for (next in listOf(ComposerProgressState(submitting = true), ComposerProgressState(sent = true))) {
            compose.runOnIdle { progress.value = next }
            compose.onNodeWithContentDescription("Повторить загрузку капчи").assertIsNotEnabled()
        }
    }

    @Test fun acceptedStepsReplaceIdleLabelButDoNotHideAnError() {
        val failure = mutableStateOf(ComposerFailureState())
        compose.setContent {
            MaterialTheme {
                ComposerStatusLine(
                    ComposerCaptchaState(selectedImages = listOf(byteArrayOf(1))),
                    ComposerScreenStatus(ComposerProgressState(), failure.value, false),
                    {},
                    loader,
                )
            }
        }
        compose.onNodeWithContentDescription("Принято значков: 1").assertExists()
        compose.onNodeWithText("Решите капчу").assertDoesNotExist()
        compose.runOnIdle { failure.value = ComposerFailureState(ReplyError.CAPTCHA) }
        compose.onNodeWithText("Капча недействительна").assertIsDisplayed()
        compose.onNodeWithContentDescription("Принято значков: 1").assertDoesNotExist()
    }
}
