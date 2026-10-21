package com.solthariel.pawchan

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.intercept.Interceptor
import com.solthariel.pawchan.composer.ComposerCaptcha
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.NewThreadCaptchaActions
import com.solthariel.pawchan.composer.stableCaptchaKeyboard
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class StableCaptchaUiTest {
    @get:Rule val compose = createComposeRule()
    private val completed = AtomicInteger()
    private lateinit var loader: ImageLoader
    private val progress = mutableStateOf(ComposerProgressState())
    private val state = mutableStateOf(ComposerCaptchaState())
    private val clicked = mutableListOf<Int>()

    @Before fun createLoader() {
        loader =
            ImageLoader
                .Builder(InstrumentationRegistry.getInstrumentation().targetContext)
                .components {
                    add(Interceptor { chain -> chain.proceed().also { completed.incrementAndGet() } })
                }.build()
        state.value =
            ComposerCaptchaState(
                challenge = EmojiCaptchaChallenge("task", image(100), (0..7).map { EmojiCaptchaOption(it, image(it)) }),
            )
    }

    @After fun closeLoader() = loader.shutdown()

    @Test fun waitingDisablesChoicesWithoutExtraSpinnerOrMovingTheFollowingButton() {
        show()
        val below = compose.onNodeWithText("Отправить").fetchSemanticsNode().boundsInRoot
        val option = compose.onNodeWithContentDescription("Вариант эмодзи 1")
        val before = option.fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { progress.value = ComposerProgressState(captchaSelecting = true) }
        option.assertIsNotEnabled()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertCountEquals(0)
        assertEquals(before, option.fetchSemanticsNode().boundsInRoot)
        assertEquals(below, compose.onNodeWithText("Отправить").fetchSemanticsNode().boundsInRoot)
        assertEquals(9, completed.get())
    }

    @Test fun reorderedChoicesStayInTheirSlotsWithoutRestartingImageRequests() {
        show()
        val before = compose.onNodeWithContentDescription("Вариант эмодзи 1").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            val old = state.value
            val incoming =
                requireNotNull(old.challenge).options.reversed().mapIndexed { i, option ->
                    option.copy(number = i, image = option.image.copyOf())
                }
            state.value = old.copy(keyboard = stableCaptchaKeyboard(old.keyboard, incoming))
        }
        compose.onNodeWithContentDescription("Вариант эмодзи 1").performClick()
        assertEquals(listOf(7), clicked)
        assertEquals(before, compose.onNodeWithContentDescription("Вариант эмодзи 1").fetchSemanticsNode().boundsInRoot)
        assertEquals(9, completed.get())
    }

    @Test fun replacingOneSymbolStartsOnlyOneNewImageRequest() {
        show()
        compose.runOnIdle {
            val old = state.value
            val incoming = requireNotNull(old.challenge).options.toMutableList()
            val replacement = image(42)
            assertFalse(incoming.any { it.image.contentEquals(replacement) })
            incoming[3] = EmojiCaptchaOption(3, replacement)
            state.value = old.copy(keyboard = stableCaptchaKeyboard(old.keyboard, incoming))
        }
        compose.waitForIdle()
        compose.waitUntil(5_000) { completed.get() >= 10 }
        compose.waitForIdle()
        assertEquals(10, completed.get())
    }

    @Test fun fewerServerChoicesLeaveEmptyCellsAndNoStaleClickableButtons() {
        show()
        val below = compose.onNodeWithText("Отправить").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            val old = state.value
            val incoming = listOf(requireNotNull(old.keyboard[7]).copy(number = 33))
            state.value = old.copy(keyboard = stableCaptchaKeyboard(old.keyboard, incoming))
        }
        compose.onNodeWithContentDescription("Вариант эмодзи 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Вариант эмодзи 8").performClick()
        assertEquals(listOf(33), clicked)
        assertEquals(below, compose.onNodeWithText("Отправить").fetchSemanticsNode().boundsInRoot)
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    ComposerCaptcha(state.value, progress.value, loader, NewThreadCaptchaActions({ clicked += it }, {}))
                    Text("Отправить")
                }
            }
        }
        compose.waitUntil(5_000) { completed.get() >= 9 }
        compose.waitForIdle()
    }

    private fun image(symbol: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff000000.toInt() or (symbol * 193))
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }
}
