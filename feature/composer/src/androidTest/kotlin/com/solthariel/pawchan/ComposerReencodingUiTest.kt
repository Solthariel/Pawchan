package com.solthariel.pawchan

import android.accessibilityservice.AccessibilityService
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.composer.AttachmentOptionsPanel
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ImageReencoding
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerReencodingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reencodingRequiresConfirmationAndBackDiscardsChanges() {
        val file = showPanel()
        openSettings()
        compose.onNodeWithText("Применить").assertIsDisplayed()
        val applyBounds = compose.onNodeWithText("Применить").fetchSemanticsNode().boundsInRoot
        val resetBounds = compose.onNodeWithContentDescription("Сбросить перекодирование").fetchSemanticsNode().boundsInRoot
        assertTrue(resetBounds.right <= applyBounds.left || resetBounds.bottom <= applyBounds.top)
        changeQuality(85f)
        compose.runOnIdle { assertNull(file.value.options.reencoding) }
        closeSettingsWithSystemBack()
        compose.runOnIdle { assertNull(file.value.options.reencoding) }
        openSettings()
        changeQuality(85f)
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertEquals(
                85,
                file.value.options.reencoding
                    ?.quality,
            )
        }
    }

    @Test fun cancelDiscardsDraftWithoutChangingAppliedSettings() {
        val original = image().let { it.copy(options = it.options.copy(reencoding = ImageReencoding(quality = 85))) }
        val file = showPanel(original)
        openSettings()
        changeQuality(35f)
        compose.onNodeWithText("PNG").performClick()
        val cancel = InstrumentationRegistry.getInstrumentation().targetContext.getString(android.R.string.cancel)
        compose.onNodeWithText(cancel).assertIsDisplayed().performClick()
        compose.onNodeWithText("Настройки перекодирования").assertDoesNotExist()
        compose.runOnIdle { assertEquals(original.options, file.value.options) }
    }

    @Test fun resetIsPendingAndBackPreservesAppliedSettings() {
        val original =
            image().let {
                it.copy(
                    options =
                        it.options.copy(
                            reencoding = ImageReencoding(jpeg = false, scaleDivisor = 2),
                            image = it.options.image.copy(previousReencoding = ImageReencoding(quality = 85)),
                            customName = "renamed",
                            spoiler = true,
                            uniqueHash = true,
                        ),
                )
            }
        val file = showPanel(original)
        openSettings()
        compose.onNodeWithText("JPEG").performClick()
        changeQuality(35f)
        compose.onNodeWithText("PNG").performClick()
        compose.onNodeWithText("8×").performClick()
        compose.onNodeWithContentDescription("Сбросить перекодирование").performClick()
        compose.onNodeWithText("Настройки перекодирования").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(original.options, file.value.options)
        }
        compose.onNodeWithText("JPEG").assertIsSelected()
        compose.onNodeWithText("100%").assertIsDisplayed()
        compose.onNodeWithText("1×").assertIsSelected()
        changeQuality(55f)
        closeSettingsWithSystemBack()
        compose.runOnIdle { assertEquals(original.options, file.value.options) }
        openSettings()
        compose.onNodeWithText("PNG").assertIsSelected()
        compose.onNodeWithText("2×").assertIsSelected()
    }

    @Test fun resetAndApplyClearsSettings() {
        val original = image().let { it.copy(options = it.options.copy(reencoding = ImageReencoding(quality = 85))) }
        val file = showPanel(original)
        openSettings()
        compose.onNodeWithContentDescription("Сбросить перекодирование").performClick()
        compose.runOnIdle { assertEquals(original.options, file.value.options) }
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertNull(file.value.options.reencoding)
            assertNull(file.value.options.previousReencoding)
        }
    }

    @Test fun resetShowsTheOriginalFormatInPendingSummary() {
        val source = ComposerAttachment(ReplyAttachment("image.png", "image/png", byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
        val original = source.copy(options = source.options.copy(reencoding = ImageReencoding()))
        showPanel(original)
        openSettings()
        compose.onNodeWithText("image.jpg", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Сбросить перекодирование").performClick()
        compose.onNodeWithText("image.png", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("PNG").assertIsSelected()
    }

    @Test fun resetAllowsApplyingNewSettingsWithoutReopening() {
        val original = image().let { it.copy(options = it.options.copy(reencoding = ImageReencoding(quality = 85))) }
        val file = showPanel(original)
        openSettings()
        compose.onNodeWithContentDescription("Сбросить перекодирование").performClick()
        compose.onNodeWithText("Настройки перекодирования").assertIsDisplayed()
        changeQuality(65f)
        compose.onNodeWithText("2×").performClick()
        compose.onNodeWithText("Применить").performClick()
        compose.onNodeWithText("Настройки перекодирования").assertDoesNotExist()
        compose.runOnIdle {
            val expected = ImageReencoding(quality = 65, scaleDivisor = 2)
            assertEquals(expected, file.value.options.reencoding)
            assertEquals(expected, file.value.options.previousReencoding)
        }
    }

    @Test fun customQualityAppliesExactlyAndPngHidesQuality() {
        val file = showPanel()
        openSettings()
        changeQuality(75f)
        compose.onNodeWithText("PNG").performClick()
        compose.onNodeWithContentDescription("Качество JPEG").assertDoesNotExist()
        compose.onNodeWithText("JPEG").performClick()
        compose.onNodeWithText("75%").assertIsDisplayed()
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertEquals(
                75,
                file.value.options.reencoding
                    ?.quality,
            )
        }
        openSettings()
        compose.onNodeWithText("75%").assertIsDisplayed()
    }

    private fun showPanel(initial: ComposerAttachment = image()): MutableState<ComposerAttachment> {
        val file = mutableStateOf(initial)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(460.dp, 700.dp)) { AttachmentOptionsPanel(file.value, true) { file.value = it } }
            }
        }
        return file
    }

    private fun closeSettingsWithSystemBack() {
        compose.onNodeWithText("Настройки перекодирования").assertIsDisplayed()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // Back belongs to the foreground dialog, not Espresso's default Activity root matcher.
        assertTrue(
            "System Back must be accepted",
            instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK),
        )
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Настройки перекодирования").fetchSemanticsNodes().isEmpty()
        }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription("Перекодировать изображение").performClick()
    }

    private fun changeQuality(quality: Float) {
        compose
            .onNodeWithContentDescription("Качество JPEG")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(quality) }
    }

    private fun image() = ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40)))
}
